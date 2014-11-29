package cl.monsoon.s1next.fragment;

import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Loader;
import android.os.Bundle;

import com.squareup.okhttp.RequestBody;

import cl.monsoon.s1next.model.mapper.ResultWrapper;
import cl.monsoon.s1next.widget.AsyncResult;
import cl.monsoon.s1next.widget.HttpPostLoader;

/**
 * Wrap {@link cl.monsoon.s1next.widget.HttpPostLoader} and ProgressDialog.
 */
public abstract class AbsPostLoaderFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<AsyncResult<ResultWrapper>>, DialogInterface.OnCancelListener {

    /**
     * The serialization (saved instance state) Bundle key representing
     * whether is loading when configuration changed.
     */
    private static final String STATE_LOADING = "loading";

    /**
     * The serialization (saved instance state) Bundle key representing
     * current loader id.
     */
    private static final String STATE_ID_LOADER = "id_loader";

    private static final int ID_LOADER = 0;

    Loader mLoader;

    /**
     * Whether {@link android.content.Loader} is loading.
     */
    Boolean mLoading = false;
    private int mLoaderId = -1;

    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mLoading = savedInstanceState.getBoolean(STATE_LOADING);
            mLoaderId = savedInstanceState.getInt(STATE_ID_LOADER);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // show ProgressDialog if Loader is still loading (works when configuration changed)
        if (mLoading) {
            if (mLoaderId == -1) {
                throw new IllegalStateException("Loader id must not be -1.");
            }

            showProgressDialog();
            mLoader = getLoaderManager().initLoader(mLoaderId, null, this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        dismissProgressDialog();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(STATE_LOADING, mLoading);
        outState.putInt(STATE_ID_LOADER, mLoaderId);
    }

    void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getProgressMessage());
            mProgressDialog.setOnCancelListener(this);
        }

        mProgressDialog.show();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        // cancel HTTP post
        // see HttpGetLoader#cancelLoad()
        if (mLoader instanceof HttpPostLoader) {
            //noinspection RedundantCast
            ((HttpPostLoader) mLoader).cancelLoad();
        } else {
            throw new ClassCastException(mLoader + " must extend HttpPostLoader.");
        }
        mLoading = false;
    }

    protected abstract CharSequence getProgressMessage();

    void dismissProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    /**
     * @param requestBody used when content changed.
     */
    void startLoader(RequestBody requestBody) {
        // We need to change the post body
        // if we have Loader before.
        mLoader = getLoaderManager().getLoader(ID_LOADER);
        if (mLoader == null) {
            mLoader = getLoaderManager().initLoader(ID_LOADER, null, this);
        } else {
            if (mLoader instanceof HttpPostLoader) {
                // pass RequestBody to change post body
                //noinspection RedundantCast
                ((HttpPostLoader) mLoader).onContentChanged(requestBody);
            } else {
                throw new ClassCastException(mLoader + " must extend HttpPostLoader.");
            }
        }
        mLoading = true;
    }

    @Override
    public Loader<AsyncResult<ResultWrapper>> onCreateLoader(int id, Bundle args) {
        mLoaderId = id;

        return null;
    }

    @Override
    public void onLoadFinished(Loader<AsyncResult<ResultWrapper>> loader, AsyncResult<ResultWrapper> data) {
        mLoading = false;
        dismissProgressDialog();
    }

    @Override
    public void onLoaderReset(Loader<AsyncResult<ResultWrapper>> loader) {

    }
}