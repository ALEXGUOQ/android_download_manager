package example.filedownload.pub;

public interface DownloadTaskListener {
    public void startDownload(String url);		// ��ʼ����
    
    public void updateProcess(String url, String process);	// ���½���
    
    public void finishDownload(String url);		// �������
}
