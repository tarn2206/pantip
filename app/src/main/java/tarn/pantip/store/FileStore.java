package tarn.pantip.store;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import tarn.pantip.util.Optional;
import tarn.pantip.util.RxUtils;
import tarn.pantip.util.Utils;

/**
 * Created by Tarn on 14-Apr-15.
 */
public class FileStore
{
    public void removeOldFiles()
    {
        RxUtils.observe(() -> {
            File root = Utils.getFileDir();
            long time = new Date().getTime() - 86400000 * 3;
            deleteFolder(root, time);
            return Optional.empty();
        }).delaySubscription(10, TimeUnit.SECONDS).subscribe();
    }

    private void deleteFolder(File dir, long time)
    {
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) return;

        for (File file : files)
        {
            if (file.isDirectory())
            {
                deleteFolder(file, time);
            }
            else if (file.lastModified() < time)
            {
                FileUtils.deleteQuietly(file);
            }
        }
    }
}