package me.lebob.pdfmultiplepagespersheetconverter;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfDocumentAdapter extends PrintDocumentAdapter {

    Context context;
    FileInputStream path;

    public PdfDocumentAdapter(Context context, FileInputStream path){
        this.context = context;
        this.path = path;
    }
    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes printAttributes1, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle extras) {
        if (cancellationSignal.isCanceled())
            layoutResultCallback.onLayoutCancelled();

        else
        {
            PrintDocumentInfo.Builder builder = new PrintDocumentInfo.Builder("file name");
            builder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build();
            layoutResultCallback.onLayoutFinished(builder.build(),!printAttributes1.equals(printAttributes1));
        }
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
        InputStream in = null;
        OutputStream out = null;
        try{
            in = path;
            out = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());

            byte[]buff = new byte[16384];
            int size;
            while ((size = in.read(buff)) >= 0 && !cancellationSignal.isCanceled()){
                out.write(buff,0,size);
            }
            if (cancellationSignal.isCanceled())
                writeResultCallback.onWriteCancelled();
            else{
                writeResultCallback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            }
        } catch (Exception e) {
            writeResultCallback.onWriteFailed(e.getMessage());
            Log.e("Harshita",e.getMessage());
            e.printStackTrace();
        }
        finally {
            try
            {
                in.close();
                out.close();
            }catch (IOException ex){
                Log.e("Harshita", ""+ex.getMessage());
            }
        }
    }
}