/*
 *  Copyright (c) 2017 Tran Le Duy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duy.pascal.frontend.file.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.duy.pascal.frontend.R;
import com.duy.pascal.frontend.common.listeners.OnItemClickListener;
import com.duy.pascal.frontend.common.task.JecAsyncTask;
import com.duy.pascal.frontend.common.task.TaskListener;
import com.duy.pascal.frontend.common.task.TaskResult;
import com.duy.pascal.frontend.common.utils.UIUtils;
import com.duy.pascal.frontend.databinding.FileExplorerFragmentBinding;
import com.duy.pascal.frontend.file.ExplorerContext;
import com.duy.pascal.frontend.file.FileActionCallback;
import com.duy.pascal.frontend.file.FileClipboard;
import com.duy.pascal.frontend.file.FileExplorerAction;
import com.duy.pascal.frontend.file.FileExplorerView;
import com.duy.pascal.frontend.file.Pref;
import com.duy.pascal.frontend.file.activities.FileExplorerActivity;
import com.duy.pascal.frontend.file.adapter.FileListItemAdapter;
import com.duy.pascal.frontend.file.adapter.PathButtonAdapter;
import com.duy.pascal.frontend.file.io.JecFile;
import com.duy.pascal.frontend.file.io.RootFile;
import com.duy.pascal.frontend.file.listener.FileListResultListener;
import com.duy.pascal.frontend.file.listener.OnClipboardPasteFinishListener;
import com.duy.pascal.frontend.file.util.FileListSorter;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Jecelyin Peng <jecelyin@gmail.com>
 */
public class FileListPagerFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, OnItemClickListener, FileExplorerView, ExplorerContext, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "FileListPagerFragment";
    private FileListItemAdapter adapter;
    @Nullable
    private JecFile path;
    private FileExplorerFragmentBinding binding;
    private PathButtonAdapter mPathAdapter;
    private boolean isRoot;
    private ScanFilesTask task;
    private FileExplorerAction action;

    public static Fragment newFragment(JecFile path) {
        FileListPagerFragment f = new FileListPagerFragment();
        Bundle b = new Bundle();
        b.putParcelable("path", path);
        f.setArguments(b);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        path = getArguments().getParcelable("path");
        binding = DataBindingUtil.inflate(inflater, R.layout.file_explorer_fragment, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FileClipboard fileClipboard = ((FileActionCallback) getActivity()).getFileClipboard();
        action = new FileExplorerAction(getContext(), this, fileClipboard, this);
        adapter = new FileListItemAdapter();
        adapter.setOnCheckedChangeListener(action);
        adapter.setOnItemClickListener(this);
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                binding.emptyLayout.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.emptyLayout.setVisibility(adapter.getItemCount() > 0 ? View.GONE : View.VISIBLE);
                    }
                });

            }
        });

        binding.pathScrollView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        mPathAdapter = new PathButtonAdapter();
        mPathAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                binding.pathScrollView.scrollToPosition(mPathAdapter.getItemCount() - 1);
            }
        });
        mPathAdapter.setPath(path);
        mPathAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                JecFile file = mPathAdapter.getItem(position);
                switchToPath(file);
            }

            @Override
            public boolean onItemLongClick(int position, View view) {
                return false;
            }
        });
        binding.pathScrollView.setAdapter(mPathAdapter);

        binding.explorerSwipeRefreshLayout.setOnRefreshListener(this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(getContext()).margin(getResources().getDimensionPixelSize(R.dimen.file_list_item_divider_left_margin), 0).build());
        binding.explorerSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                binding.explorerSwipeRefreshLayout.setRefreshing(true);
            }
        });
        binding.nameFilterEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        view.post(new Runnable() {
            @Override
            public void run() {
                isRoot = Pref.getInstance(getContext()).isRootable();
                onRefresh();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onDestroyView() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        pref.unregisterOnSharedPreferenceChangeListener(this);
        if (action != null) {
            action.destroy();
        }
        super.onDestroyView();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (task != null) {
            task.cancel(true);
            task = null;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.paste_menu) {
            final FileClipboard fileClipboard = ((FileExplorerActivity) getActivity()).getFileClipboard();
            fileClipboard.paste(getContext(), getCurrentDirectory(), new OnClipboardPasteFinishListener() {
                @Override
                public void onFinish(int count, String error) {
                    onRefresh();
                    fileClipboard.showPasteResult(getContext(), count, error);
                }
            });
            item.setVisible(false);
        } else if (item.getItemId() == R.id.add_folder_menu) {
            action.doCreateFolder();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        Log.d(TAG, "onRefresh() called");

        UpdateRootInfo updateRootInfo = new UpdateRootInfo() {

            @Override
            public void onUpdate(JecFile f) {
                path = f;
            }
        };
        task = new ScanFilesTask(getActivity(), path, isRoot, updateRootInfo);
        task.setTaskListener(new TaskListener<JecFile[]>() {
            @Override
            public void onCompleted() {
                if (binding.explorerSwipeRefreshLayout != null) {
                    binding.explorerSwipeRefreshLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            binding.explorerSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, 300);
                }
            }

            @Override
            public void onSuccess(JecFile[] result) {
                if (adapter != null) {
                    adapter.setData(result);
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                if (binding.explorerSwipeRefreshLayout == null) {
                    return;
                }
                binding.explorerSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        binding.explorerSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 300);
                UIUtils.toast(getContext(), e);
            }
        });
        task.execute();
    }

    @Override
    public void onItemClick(int position, View view) {
        try {
            JecFile file = adapter.getItem(position);
            FileActionCallback callback = (FileActionCallback) getActivity();
            if (!callback.onSelectFile(new File(file.getPath()))) {
                if (file.isDirectory()) {
                    switchToPath(file);
                }
            }
        } catch (ClassCastException e) {
        }
    }

    @Override
    public boolean onItemLongClick(int position, View view) {
//        try {
//            JecFile file = adapter.getItem(position);
//            FileActionCallback callback = (FileActionCallback) getActivity();
//            return callback.onFileLongClick(new File(file.getPath()));
//        } catch (ClassCastException ignored) {
//        }
        return false;
    }

    public boolean onBackPressed() {
        JecFile parent = path.getParentFile();
        if (parent == null || parent.getPath().startsWith(path.getPath())) {
            switchToPath(parent);
            return true;
        }
        return false;
    }

    private void switchToPath(JecFile file) {
        path = file;
        mPathAdapter.setPath(file);
        Pref.getInstance(getContext()).setLastOpenPath(file.getPath());
        onRefresh();
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        return ((AppCompatActivity) getActivity()).startSupportActionMode(callback);
    }

    @Override
    public void setSelectAll(boolean checked) {
        adapter.checkAll(checked);
    }

    @Override
    public void refresh() {
        onRefresh();
    }

    @Override
    public void finish() {
        getActivity().finish();
    }

    @Override
    public JecFile getCurrentDirectory() {
        return path;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        onRefresh();
    }

    private interface UpdateRootInfo {
        void onUpdate(JecFile path);
    }

    private static class ScanFilesTask extends JecAsyncTask<Void, Void, JecFile[]> {
        private final UpdateRootInfo updateRootInfo;
        private final Context context;
        private JecFile path;
        private boolean isRoot;

        private ScanFilesTask(Context context, JecFile path, boolean isRoot, UpdateRootInfo updateRootInfo) {
            this.context = context.getApplicationContext();
            this.path = path;
            this.isRoot = isRoot;
            this.updateRootInfo = updateRootInfo;
        }

        @Override
        protected void onRun(final TaskResult<JecFile[]> taskResult, Void... params) throws Exception {
            Pref pref = Pref.getInstance(context);
            final boolean showHiddenFiles = pref.isShowHiddenFiles();
            final int sortType = pref.getFileSortType();
            if (isRoot && !(path instanceof RootFile) && !path.getPath().startsWith(Environment.getExternalStorageDirectory().getPath())) {
                path = new RootFile(path.getPath());
            }
            updateRootInfo.onUpdate(path);
            path.listFiles(new FileListResultListener() {
                @Override
                public void onResult(JecFile[] result) {
                    if (result.length == 0) {
                        taskResult.setResult(result);
                        return;
                    }
                    if (!showHiddenFiles) {
                        List<JecFile> list = new ArrayList<>(result.length);
                        for (JecFile file : result) {
                            if (file.getName().charAt(0) == '.') {
                                continue;
                            }
                            list.add(file);
                        }
                        result = new JecFile[list.size()];
                        list.toArray(result);
                    }
                    Arrays.sort(result, new FileListSorter(true, sortType, true));
                    taskResult.setResult(result);
                }
            });
        }
    }
}
