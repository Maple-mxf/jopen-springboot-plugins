package automation;

import com.google.common.base.Verify;
import com.jcraft.jsch.*;
import com.onepushing.springboot.support.exception.ServiceException;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.util.Properties;

import static com.jcraft.jsch.ChannelSftp.SSH_FX_NO_SUCH_FILE;

/**
 * @author maxuefeng
 */
@Deprecated
public final class FileTransfer {

    private JSch jSch;
    private ChannelSftp channelSftp;
    private Session session;

    public FileTransfer(Adapter adapter) throws JSchException {
        Verify.verify(adapter != null);
        this.jSch = new JSch();

        if (Adapter.AuthType.PRIVATE_KEY.equals(adapter.getAuthType())) {
            Verify.verify(adapter.getPrvKeyFile() != null);
            jSch.addIdentity(adapter.getPrvKeyFile());
        }

        this.session = jSch.getSession(adapter.getUsername(), adapter.getHost(), adapter.getConnectPort());
        this.session.setPassword(adapter.getPassword());
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        this.session.setConfig(config);
        this.session.connect();
        this.channelSftp = (ChannelSftp) session.openChannel("sftp");
        this.channelSftp.connect();
    }

    public FileTransferFuture transfer(String distDir, File file, boolean noDistDirCreate) {
        try {
            channelSftp.cd(distDir);
        } catch (SftpException e) {
            if (noDistDirCreate) {
                if (SSH_FX_NO_SUCH_FILE == e.id) {
                    try {
                        channelSftp.mkdir(distDir);
                        channelSftp.cd(distDir);
                    } catch (SftpException se) {
                        se.printStackTrace();
                        return FileTransferFuture.of(e.getMessage());
                    }
                }
            } else throw new ServiceException(String.format("no such dir %s", distDir));
        }

        try {
            InputStream in = new FileInputStream(file);
            channelSftp.put(in, file.getName());
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return FileTransferFuture.of(e.getMessage());
        }
        return FileTransferFuture.of();
    }

    public FileTransferFuture transfer(String distDir, InputStream in, String fileName, boolean noDistDirCreate) {
        try {
            channelSftp.cd(distDir);
        } catch (SftpException se) {
            if (noDistDirCreate) {
                if (SSH_FX_NO_SUCH_FILE == se.id) {
                    try {
                        channelSftp.mkdir(distDir);
                        channelSftp.cd(distDir);
                    } catch (SftpException sse) {
                        sse.printStackTrace();
                        return FileTransferFuture.of(sse.getMessage());
                    }
                }
            } else throw new ServiceException(String.format("no such dir %s", distDir));
        }
        try {
            channelSftp.put(in,fileName);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return FileTransferFuture.of(e.getMessage());
        }
        return FileTransferFuture.of();
    }

    public void download(String distDir, String downloadFile, String saveFile) throws IOException, SftpException {
        channelSftp.cd(distDir);
        File file = new File(saveFile);
        boolean bFile = file.exists();
        if (!bFile) file.mkdirs();

        OutputStream out = new FileOutputStream(new File(saveFile, downloadFile));
        channelSftp.get(downloadFile, out);

        out.flush();
        out.close();
    }

    public void releaseSource() {
        this.session.disconnect();
        this.channelSftp.disconnect();
    }

    @Getter
    @Setter
    public static class FileTransferFuture {
        private Boolean success;
        private String errMsg;

        public static FileTransferFuture of() {
            FileTransferFuture fileTransferFuture = new FileTransferFuture();
            fileTransferFuture.setSuccess(true);
            return fileTransferFuture;
        }

        public static FileTransferFuture of(String msg) {
            FileTransferFuture fileTransferFuture = new FileTransferFuture();
            fileTransferFuture.setSuccess(false);
            fileTransferFuture.setErrMsg(msg);
            return fileTransferFuture;
        }
    }
}
