/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */
package com.haulmont.cuba.web.app.ui.frame;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.core.global.TimeProvider;
import com.haulmont.cuba.gui.components.AbstractWindow;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.FileUploadField;
import com.haulmont.cuba.gui.components.Table;
import com.haulmont.cuba.gui.components.actions.RemoveAction;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.upload.FileUploadingAPI;
import com.haulmont.cuba.web.app.FileDownloadHelper;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Map;

/**
 * @author tulupov
 * @version $Id$
 */
public class FileFrameController extends AbstractWindow {

    private CollectionDatasource ds;

    private FileUploadField uploadField;

    private FileDescriptor fd;

    private Table filesTable;

    @Override
    public void init(Map<String, Object> params) {
        super.init(params);

        uploadField = getComponent("uploadField");
        filesTable = getComponent("files");
        initGeneratedColumn();
        ds = getDsContext().get("filesDs");
        Button remove = getComponent("remove");
        remove.setAction(new RemoveAction(filesTable, false));

        uploadField.addListener(new FileUploadField.Listener() {
            @Override
            public void uploadStarted(Event event) {
                uploadField.setEnabled(false);
            }

            @Override
            public void uploadFinished(Event event) {
                uploadField.setEnabled(true);
            }

            @Override
            public void uploadSucceeded(Event event) {
                fd = new FileDescriptor();
                fd.setName(uploadField.getFileName());
                fd.setExtension(FilenameUtils.getExtension(uploadField.getFileName()));

                FileUploadingAPI fileUploading = AppBeans.get(FileUploadingAPI.NAME);
                File file = fileUploading.getFile(uploadField.getFileId());
                fd.setSize((int)file.length());

                fd.setCreateDate(TimeProvider.currentTimestamp());
                saveFile();
                ds.addItem(fd);
                showNotification(MessageProvider.getMessage(getClass(), "uploadSuccess"), NotificationType.HUMANIZED);
            }

            @Override
            public void uploadFailed(Event event) {
                showNotification(MessageProvider.getMessage(getClass(), "uploadUnsuccess"), NotificationType.HUMANIZED);
            }

            public void updateProgress(long readBytes, long contentLength) {
            }
        });
    }

    public void initGeneratedColumn() {
        if (filesTable.getDatasource().getState().equals(Datasource.State.VALID)) {
            FileDownloadHelper.initGeneratedColumn(filesTable);
        }
    }

    private void saveFile() {
        FileUploadingAPI fileUploading = AppBeans.get(FileUploadingAPI.NAME);
        try {
            fileUploading.putFileIntoStorage(uploadField.getFileId(), fd);
        } catch (FileStorageException e) {
            throw new RuntimeException(e);
        }
    }
}
