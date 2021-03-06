package fr.xgouchet.deezer.muzei.app;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

import fr.xgouchet.deezer.muzei.R;
import fr.xgouchet.deezer.muzei.data.AlbumDao;
import fr.xgouchet.deezer.muzei.data.AlbumInfo;
import fr.xgouchet.deezer.muzei.util.Constants;


/**
 * 
 * @author Xavier Gouchet
 * 
 */
public class SelectCustomActivity extends Activity {
    
    private DeezerConnect mConnect;
    
    private AlbumDao mAlbumDao;
    private GridView mGridView;
    private AlbumAdapter mAlbumAdapter;
    private View mAddAlbumView;
    
    
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        
        
        setContentView(R.layout.activity_custom);
        
        mAlbumDao = new AlbumDao(this);
        
        // setup gridview 
        mGridView = (GridView) findViewById(android.R.id.list);
        mGridView.setEmptyView(findViewById(android.R.id.empty));
        mGridView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        mGridView.setMultiChoiceModeListener(mAlbumSelectionListener);
        
        // setup add album button
        mAddAlbumView = findViewById(R.id.add_new_album);
        mAddAlbumView.setOnClickListener(mAddAlbumClickListener);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        mConnect = new DeezerConnect(Constants.APP_ID);
        new SessionStore().restore(mConnect, this);
        
        List<AlbumInfo> albums = mAlbumDao.getSelectedAlbums();
        mAlbumAdapter = new AlbumAdapter(this, albums);
        mGridView.setAdapter(mAlbumAdapter);
        
        
    }
    
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Add Album Button listener
    //////////////////////////////////////////////////////////////////////////////////////
    
    private OnClickListener mAddAlbumClickListener = new OnClickListener() {
        
        @Override
        public void onClick(final View v) {
            Intent intent = new Intent(SelectCustomActivity.this, SearchAlbumActivity.class);
            startActivity(intent);
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Long Click Listener
    //////////////////////////////////////////////////////////////////////////////////////
    private MultiChoiceModeListener mAlbumSelectionListener = new MultiChoiceModeListener() {
        
        private final List<AlbumInfo> mAlbums = new ArrayList<AlbumInfo>();
        
        private ActionMode mActionMode;
        
        @Override
        public boolean onPrepareActionMode(final ActionMode mode, final Menu menu) {
            Log.i("ActionMode", "onPrepareActionMode");
            
            mAlbums.clear();
            return false;
        }
        
        @Override
        public void onDestroyActionMode(final ActionMode mode) {
            Log.i("ActionMode", "onDestroyActionMode");
            
        }
        
        @Override
        public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
            Log.i("ActionMode", "onCreateActionMode");
            getMenuInflater().inflate(R.menu.custom_action, menu);
            mActionMode = mode;
            return true;
        }
        
        @Override
        public boolean onActionItemClicked(final ActionMode mode, final MenuItem item) {
            Log.i("ActionMode", "onActionItemClicked");
            if (item.getItemId() == R.id.action_delete) {
                deleteSelectedAlbums();
                mActionMode.finish();
                return true;
            }
            
            
            return false;
        }
        
        @Override
        public void onItemCheckedStateChanged(final ActionMode mode, final int position,
                final long id,
                final boolean checked) {
            Log.i("ActionMode", "onItemCheckedStateChanged");
            
            AlbumInfo info = mAlbumAdapter.getItem(position);
            if (checked) {
                mAlbums.add(info);
            } else {
                mAlbums.remove(info);
            }
            
            mActionMode.setTitle(Integer.toString(mAlbums.size()));
        }
        
        private void deleteSelectedAlbums() {
            for (AlbumInfo info : mAlbums) {
                mAlbumDao.removeAlbum(info);
                mAlbumAdapter.remove(info);
            }
            
            
            mAlbumAdapter.notifyDataSetChanged();
            
        }
    };
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Info Adapter
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class AlbumAdapter extends ArrayAdapter<AlbumInfo> {
        
        private final LayoutInflater mLayoutInflater;
        
        public AlbumAdapter(final Context context, final List<AlbumInfo> list) {
            super(context, R.layout.item_album, list);
            mLayoutInflater = LayoutInflater.from(context);
        }
        
        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            AlbumViewHolder holder;
            View view = convertView;
            
            if (view == null) {
                view = mLayoutInflater.inflate(R.layout.item_album, parent, false);
                
                holder = new AlbumViewHolder();
                holder.title = (TextView) view.findViewById(R.id.album_title);
                holder.cover = (ImageView) view.findViewById(R.id.album_cover);
                
                view.setTag(holder);
            } else {
                holder = (AlbumViewHolder) view.getTag();
            }
            
            AlbumInfo album = getItem(position);
            holder.albumId = album.id;
            holder.title.setText(album.title);
            holder.cover.setImageResource(R.drawable.album_default);
            
            File cacheFolder = new File(getContext().getCacheDir(), "thumbs");
            cacheFolder.mkdirs();
            File cacheFile = new File(cacheFolder, Long.toString(album.id));
            
            if (cacheFile.exists()) {
                Picasso.with(getContext()).load(cacheFile).placeholder(R.drawable.album_default)
                        .into(holder.cover);
            } else {
                Picasso.with(getContext()).load(album.cover).placeholder(R.drawable.album_default)
                        .into(new AlbumTarget(album, holder, cacheFile));
            }
            
            return view;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Simple view holder
    //////////////////////////////////////////////////////////////////////////////////////
    private class AlbumViewHolder {
        
        public TextView title;
        public ImageView cover;
        public long albumId;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////
    // Album Target (for Picasso)
    //////////////////////////////////////////////////////////////////////////////////////
    
    private class AlbumTarget implements Target {
        
        private final AlbumInfo mInfo;
        private final AlbumViewHolder mHolder;
        private final File mFile;
        
        public AlbumTarget(final AlbumInfo info, final AlbumViewHolder holder, final File file) {
            mInfo = info;
            mHolder = holder;
            mFile = file;
        }
        
        
        @Override
        public void onPrepareLoad(final Drawable arg0) {
            
        }
        
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, final LoadedFrom from) {
            
            if (mHolder.albumId == mInfo.id) {
                mHolder.cover.setImageBitmap(bitmap);
            }
            
            FileOutputStream output = null;
            
            try {
                output = new FileOutputStream(mFile);
                
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                output.flush();
            }
            catch (Exception e) {
                mFile.delete();
            }
            finally {
                try {
                    output.close();
                }
                catch (Exception e) {
                    // 
                }
            }
        }
        @Override
        public void onBitmapFailed(final Drawable arg0) {
            // TODO Auto-generated method stub
            
        }
        
    }
}
