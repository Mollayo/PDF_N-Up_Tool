package me.lebob.pdfmultiplepagespersheetconverter;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import me.lebob.pdfmultiplepagespersheetconverter.databinding.FragmentFirstBinding;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FirstFragment extends Fragment {

    private static final int SELECT_PDF_REQUEST_CODE=200;

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.numberOfColumns.setText("2");
        binding.numberOfRows.setText("3");
        binding.generatePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                startActivityForResult(intent, SELECT_PDF_REQUEST_CODE);
                //startActivityForResult(Intent.createChooser(intent,"Choose a PDf file"), SELECT_PDF_REQUEST_CODE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null)
            return;

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
        // get the new value from Intent data
        Uri uri = Uri.parse(data.getDataString());

        try {
            PdfReader reader = new PdfReader(getActivity().getContentResolver().openInputStream(uri));
            //File directory = Environment.getExternalStorageDirectory();
            File directory = getActivity().getFilesDir();
            String fileName="result.pdf";
            File file = new File(directory, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            PdfWriter writer = new PdfWriter(outputStream);

            // Creating a PdfDocument objects
            PdfDocument destpdf = new PdfDocument(writer);
            PdfDocument srcPdf = new PdfDocument(reader);

            // Opening a page from the existing PDF
            PageSize nUpPageSize = PageSize.A4;
            PdfCanvas canvas = null;

            int nbRows=3;
            int nbColumns=2;
            try {
                nbRows=Integer.parseInt(binding.numberOfRows.getText().toString());
                nbColumns=Integer.parseInt(binding.numberOfColumns.getText().toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
            int nbPagesPerSheet=nbRows*nbColumns;
            for (int pageIdx=0;pageIdx<srcPdf.getNumberOfPages();pageIdx=pageIdx+nbPagesPerSheet) {
                // Add a new page
                PdfPage page = destpdf.addNewPage(nUpPageSize);
                canvas = new PdfCanvas(page);

                // Scale page
                PdfPage origPage = srcPdf.getPage(pageIdx+1);
                Rectangle orig = origPage.getPageSize();

                float scale=nUpPageSize.getWidth()/orig.getWidth()/(float)nbColumns;
                if (nUpPageSize.getHeight()/orig.getHeight()/(float)nbRows<scale)
                    scale=nUpPageSize.getHeight()/orig.getHeight()/(float)nbRows;
                AffineTransform transformationMatrix = AffineTransform.getScaleInstance(
                        scale,
                        scale);
                canvas.concatMatrix(transformationMatrix);

                // Get the next 8 pages from source file
                PdfFormXObject pageCopy[]=new PdfFormXObject[nbRows*nbColumns];
                for(int i=0;i<nbRows*nbColumns;i++)
                    pageCopy[i]=null;
                for (int i=0;i<nbPagesPerSheet;i++) {
                    if (pageIdx+i>=srcPdf.getNumberOfPages())
                        pageCopy[i] =null;
                    else {
                        PdfPage origPageTmp = srcPdf.getPage(pageIdx + i + 1);
                        pageCopy[i] = origPageTmp.copyAsFormXObject(destpdf);
                    }
                }

                float rowHeight=nUpPageSize.getHeight()/nbRows/scale;
                float columnWidth=nUpPageSize.getWidth()/nbColumns/scale;
                for (int row=0;row<nbRows;row++)
                    for (int column=0;column<nbColumns;column++) {
                        if (pageCopy[row*nbColumns+column] != null)
                            canvas.addXObjectAt(pageCopy[row*nbColumns+column], column * columnWidth, (nbRows-row-1) * rowHeight);
                    }
            }

            // closing the documents
            destpdf.close();
            srcPdf.close();

            // Printing the new PDF
            printPDF(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printPDF(String fileName){
        PrintManager printManager=(PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
        try
        {
            File directory = getActivity().getFilesDir();
            File file = new File(directory, fileName);
            FileInputStream inputStream = new FileInputStream(file);
            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(getContext(), inputStream);
            printManager.print("Document", printAdapter,new PrintAttributes.Builder().build());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}