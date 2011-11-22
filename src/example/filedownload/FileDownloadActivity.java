package example.filedownload;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import example.filedownload.pub.DownloadTask;
import example.filedownload.pub.DownloadTaskListener;

/**
 * AsyncTask + HttpURLConnection (���̶߳ϵ�)
 * @author SuetMing
 * 2011-11-22	
 */

public class FileDownloadActivity extends ListActivity {
    /** Called when the activity is first created. */
    private static final String TAG = "FileDownloadActivity";
    private static final int MSG_START_DOWNLOAD 	= 1;
    private static final int MSG_STOP_DOWNLOAD 	= 2;
    private static final int MSG_PAUSE_DOWNLOAD 	= 3;
    private static final int MSG_CONTINUE_DOWNLOAD 	= 4;
    private static final int MSG_INSTALL_APK	 	= 5;
    private static final int MSG_CLOSE_ALL_DOWNLOAD_TASK 	= 6;
    
    private DownloadTask tasks[];
    private ListAdapter adapter;
    
    private DownloadTaskListener downloadListener = new DownloadTaskListener() {

	@Override
	public void startDownload(String url) {
	    for(int i = 0; i < Utils.url.length; i++) {
		if (Utils.url[i].equalsIgnoreCase(url)) {
		    FileDownloadActivity.this.startDownload(i);
		}		
	    }    
	}

	@Override
	public void updateProcess(String url, String process) {
	    for(int i = 0; i < Utils.url.length; i++) {
		if (Utils.url[i].equalsIgnoreCase(url)) {
		    FileDownloadActivity.this.updateDownload(i);
		}		
	    } 
	}

	@Override
	public void finishDownload(String url) {
	    
	    for(int i = 0; i < Utils.url.length; i++) {
		if (Utils.url[i].equalsIgnoreCase(url)) {
		    Button btnStart = (Button)adapter.viewList.get(i).findViewById(R.id.btn_start);
		    Button btnPause = (Button)adapter.viewList.get(i).findViewById(R.id.btn_pause);
		    Button btnStop = (Button)adapter.viewList.get(i).findViewById(R.id.btn_stop);
		    Button btnContinue = (Button)adapter.viewList.get(i).findViewById(R.id.btn_continue);

		    btnStart.setVisibility(0);
		    btnPause.setVisibility(8);
		    btnStop.setVisibility(8);
		    btnContinue.setVisibility(8);
		    FileDownloadActivity.this.installAPK(i);
		}		
	    } 
	}
       
    };
    
    private Runnable runnable = new Runnable() {
        
        @Override
        public void run() {
	    while (!Utils.isNetworkAvailabel(FileDownloadActivity.this)) {
		Toast.makeText(FileDownloadActivity.this, "�����ѶϿ�", Toast.LENGTH_LONG);
		Message msg = new Message();
		msg.what = MSG_CLOSE_ALL_DOWNLOAD_TASK;
		handler.sendMessage(msg);		
		try {
		    Thread.sleep(10000);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		} 
	    } 	
        }
    };
    
    private Handler handler = new Handler() {
	public void handleMessage(Message msg) { 
	    switch (msg.what) { 
	    case MSG_START_DOWNLOAD:
		startDownload(msg.arg1);
		break;
	    case MSG_STOP_DOWNLOAD:
		stopDownload(msg.arg1);
		break;
	    case MSG_PAUSE_DOWNLOAD:
		pauseDownload(msg.arg1);
		break;
	    case MSG_CONTINUE_DOWNLOAD:
		continueDownload(msg.arg1);
		break;
	    case MSG_INSTALL_APK:
		Button btnStart = (Button)adapter.viewList.get(msg.arg1).findViewById(R.id.btn_start);
		Button btnPause = (Button)adapter.viewList.get(msg.arg1).findViewById(R.id.btn_pause);
		Button btnStop = (Button)adapter.viewList.get(msg.arg1).findViewById(R.id.btn_stop);
		Button btnContinue = (Button)adapter.viewList.get(msg.arg1).findViewById(R.id.btn_continue);

	        btnStart.setVisibility(0);
	        btnPause.setVisibility(8);
	        btnStop.setVisibility(8);
	        btnContinue.setVisibility(8);
		installAPK(msg.arg1);
		break;
	    case MSG_CLOSE_ALL_DOWNLOAD_TASK:
		for(int i = 0; i < Utils.url.length; i++) {
		    if (tasks[i] != null) {
			tasks[i].onCancelled();
			tasks[i] = null;
		    }
		}
		break;
	    }
	}
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        adapter = new ListAdapter(this);
        setListAdapter(adapter);
             
        tasks = new DownloadTask[Utils.url.length];
        handler.post(runnable);
    }
    
    public void updateDownload(int viewPos) {
        View convertView = adapter.getView(viewPos, getListView(), null);
        ProgressBar pb = (ProgressBar)convertView.findViewById(R.id.progressBar);
        
        pb.setProgress((int) tasks[viewPos].getDownloadPercent());
        
        TextView view = (TextView) convertView.findViewById(R.id.progress_text_view);
        view.setText( "" +
        (int) tasks[viewPos].getDownloadPercent() + "%" + " " + 
        tasks[viewPos].getDownloadSpeed() + "kbps" + " " + 
        Utils.size(tasks[viewPos].getDownloadSize()) + "/" + Utils.size(tasks[viewPos].getTotalSize()));
        
        Log.i(TAG,viewPos + " " + (int) tasks[viewPos].getDownloadPercent());
    }
    
    public void startDownload(int viewPos) {
	    if (!Utils.isSDCardPresent()) {
		Toast.makeText(this, "δ����SD��", Toast.LENGTH_LONG);
		return;
	    }
	    
	    if (!Utils.isSdCardWrittenable()) {
		Toast.makeText(this, "SD�����ܶ�д", Toast.LENGTH_LONG);
		return;
	    }
	    
	    if (tasks[viewPos] != null) {
		tasks[viewPos].onCancelled();
		tasks[viewPos] = null;
	    }
	    File file = new File(Utils.APK_ROOT + Utils.getFileNameFromUrl(Utils.url[viewPos]));
	    if (file.exists()) file.delete();			
	    try {
		tasks[viewPos] = new DownloadTask(this,
		    Utils.url[viewPos], 
		    Utils.APK_ROOT, Utils.getFileNameFromUrl(Utils.url[viewPos]),
		        downloadListener);
		tasks[viewPos].execute();
	    } catch (MalformedURLException e) {
		e.printStackTrace();
	    }	    
    }
    
    public void pauseDownload(int viewPos) {
	    if (tasks[viewPos] != null) {
		tasks[viewPos].onCancelled();
	    }
	    
	    tasks[viewPos] = null;
    }
    
    public void stopDownload(int viewPos) {
	    File file = new File(Utils.APK_ROOT + Utils.getFileNameFromUrl(Utils.url[viewPos]));
	    if (file.exists()) file.delete();
	    
	    if (tasks[viewPos] != null) {
		tasks[viewPos].onCancelled();
	    }
	    
	    tasks[viewPos] = null;
    }
    
    public void continueDownload(int viewPos) {
	    if (tasks[viewPos] == null) { 
		try {
		    tasks[viewPos] = new DownloadTask(this,
		    	    Utils.url[viewPos], 
		    	    Utils.APK_ROOT, Utils.getFileNameFromUrl(Utils.url[viewPos]),
		    	    downloadListener);
		    tasks[viewPos].execute();
		} catch (MalformedURLException e) {
		    e.printStackTrace();
		}		
	    }	
    }
    
    public void installAPK(int viewPos) {
	if (tasks[viewPos] != null) {
	    tasks[viewPos] = null;
	}
//	Utils.installAPK(FileDownloadActivity.this, Utils.url[viewPos]);
	
	Intent intent = new Intent(FileDownloadActivity.this, ImageActivity.class);
	intent.putExtra("url", viewPos);
	startActivity(intent);
    }

    private class ListAdapter extends BaseAdapter {
	private Context context;
	public List<View> viewList = new ArrayList<View>();
	
	public ListAdapter(Context context) {
	    this.context = context;
	}
	
	@Override
	public int getCount() {
	    return Utils.url.length;
	}

	@Override
	public Object getItem(int position) {
	    return position;
	}

	@Override
	public long getItemId(int position) {
	    return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (position < viewList.size()) {
		return viewList.get(position);
	    }
	    
	    if (convertView == null) {
		convertView = View.inflate(this.context, R.layout.list_item, null);
		viewList.add(convertView);
		
		Button btnStart = (Button)convertView.findViewById(R.id.btn_start);
		Button btnPause = (Button)convertView.findViewById(R.id.btn_pause);
		Button btnStop = (Button)convertView.findViewById(R.id.btn_stop);
		Button btnContinue = (Button)convertView.findViewById(R.id.btn_continue);

	        btnStart.setVisibility(0);
	        btnPause.setVisibility(8);
	        btnStop.setVisibility(8);
	        btnContinue.setVisibility(8);
		
		btnStart.setOnClickListener(new BtnListener(position));
		btnPause.setOnClickListener(new BtnListener(position));
		btnStop.setOnClickListener(new BtnListener(position));
		btnContinue.setOnClickListener(new BtnListener(position));		
	    }
	
	    return convertView;
	}
	
	private class BtnListener implements View.OnClickListener {
	    int viewPos;
	    public BtnListener(int pos) {
		this.viewPos = pos;
	    }
	    
	    @Override
	    public void onClick(View v) {
		Message message;
		switch(v.getId()) {
		case R.id.btn_continue:
		{
		    message = new Message();  
	            message.what = MSG_CONTINUE_DOWNLOAD;  
	            message.arg1 = viewPos;
	            handler.sendMessage(message);
	            
	            Button btnStart = (Button)viewList.get(viewPos).findViewById(R.id.btn_start);
	            Button btnPause = (Button)viewList.get(viewPos).findViewById(R.id.btn_pause);
	            Button btnStop = (Button)viewList.get(viewPos).findViewById(R.id.btn_stop);
	            Button btnContinue = (Button)viewList.get(viewPos).findViewById(R.id.btn_continue);

	            // ���ð�ť�ؼ��Ŀɼ���  0 �ɼ���4 ��ռλ���ɼ� ��8  ռλ���ɼ�
	            btnStart.setVisibility(8);
	            btnPause.setVisibility(0);
	            btnStop.setVisibility(8);
	            btnContinue.setVisibility(8);
		}

		    break;
		case R.id.btn_pause:
		{
		    message = new Message();  
	            message.what = MSG_PAUSE_DOWNLOAD;  
	            message.arg1 = viewPos;
	            handler.sendMessage(message);
	            
	            Button btnStart = (Button)viewList.get(viewPos).findViewById(R.id.btn_start);
	            Button btnPause = (Button)viewList.get(viewPos).findViewById(R.id.btn_pause);
	            Button btnStop = (Button)viewList.get(viewPos).findViewById(R.id.btn_stop);
	            Button btnContinue = (Button)viewList.get(viewPos).findViewById(R.id.btn_continue);

	            btnStart.setVisibility(8);
	            btnPause.setVisibility(8);
	            btnStop.setVisibility(8);
	            btnContinue.setVisibility(0);
		}	            
		    break;
		case R.id.btn_start:
		{
		    message = new Message();  
	            message.what = MSG_START_DOWNLOAD;  
	            message.arg1 = viewPos;
	            handler.sendMessage(message);
	            Button btnStart = (Button)viewList.get(viewPos).findViewById(R.id.btn_start);
	            Button btnPause = (Button)viewList.get(viewPos).findViewById(R.id.btn_pause);
	            Button btnStop = (Button)viewList.get(viewPos).findViewById(R.id.btn_stop);
	            Button btnContinue = (Button)viewList.get(viewPos).findViewById(R.id.btn_continue);

	            btnStart.setVisibility(8);
	            btnPause.setVisibility(0);
	            btnStop.setVisibility(8);
	            btnContinue.setVisibility(8);
		}
		    break;
		case R.id.btn_stop:
		{
		    message = new Message();  
	            message.what = MSG_STOP_DOWNLOAD;  
	            message.arg1 = viewPos;
	            handler.sendMessage(message);
	            Button btnStart = (Button)viewList.get(viewPos).findViewById(R.id.btn_start);
	            Button btnPause = (Button)viewList.get(viewPos).findViewById(R.id.btn_pause);
	            Button btnStop = (Button)viewList.get(viewPos).findViewById(R.id.btn_stop);
	            Button btnContinue = (Button)viewList.get(viewPos).findViewById(R.id.btn_continue);

	            btnStart.setVisibility(0);
	            btnPause.setVisibility(0);
	            btnStop.setVisibility(8);
	            btnContinue.setVisibility(8);
		}
		    break;
		}
	    } 
	}
	
    }
}