package example.filedownload.pub;

public interface DownloadListener {
    public void updateProcess(DownloadMgr mgr);			// ���½���
    public void finishDownload(DownloadMgr mgr);			// �������
}
