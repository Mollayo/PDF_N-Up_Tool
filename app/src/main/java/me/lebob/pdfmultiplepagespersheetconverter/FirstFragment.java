package me.lebob.pdfmultiplepagespersheetconverter;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import me.lebob.pdfmultiplepagespersheetconverter.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private static final int SELECT_PDF_REQUEST_CODE=200;

    private FragmentFirstBinding binding;
    private int mIntMin=1, mIntMax=100;
    private boolean nbColumnsValid=true,nbRowsValid=true;

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

        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            generatePDF(data);
                        }
                    }
                });

        // Define a filter for the EditText (nb of columns and nb of rows)
        InputFilter intFilter = new InputFilter() {
            @Override
            public CharSequence filter (CharSequence source , int start , int end , Spanned dest ,int dstart , int dend) {
                try {
                    int input = Integer. parseInt (dest.toString() + source.toString()) ;
                    if (isInRange( mIntMin , mIntMax , input))
                        return null;
                } catch (NumberFormatException e) {
                    e.printStackTrace() ;
                }
                return "" ;
            }
            private boolean isInRange ( int a , int b , int c) {
                return b > a ? c >= a && c <= b : c >= b && c <= a ;
            }
        };
        binding.numberOfColumns.setText("2");
        binding.numberOfColumns.setFilters(new InputFilter[]{intFilter});
        binding.numberOfColumns.addTextChangedListener(new TextWatcher()  {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.numberOfColumns.getText().toString().length() <= 0) {
                    binding.numberOfColumns.setError("Enter a value between 1 and 100");
                    nbColumnsValid=false;
                } else {
                    binding.numberOfColumns.setError(null);
                    nbColumnsValid=true;
                }
                binding.generatePdf.setEnabled(nbColumnsValid&&nbRowsValid);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        binding.numberOfRows.setText("3");
        binding.numberOfRows.setFilters(new InputFilter[]{intFilter});
        binding.numberOfRows.addTextChangedListener(new TextWatcher()  {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                if (binding.numberOfRows.getText().toString().length() <= 0) {
                    binding.numberOfRows.setError("Enter a value between 1 and 100");
                    nbRowsValid=false;
                } else {
                    binding.numberOfRows.setError(null);
                    nbRowsValid=true;
                }
                binding.generatePdf.setEnabled(nbColumnsValid&&nbRowsValid);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        binding.generatePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("application/pdf");
                //startActivityForResult(intent, SELECT_PDF_REQUEST_CODE);
                //startActivityForResult(Intent.createChooser(intent,"Choose a PDf file"), SELECT_PDF_REQUEST_CODE);
                someActivityResultLauncher.launch(intent);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void generatePDF(Intent data) {
        if (data == null)
            return;

        // get the new value from Intent data
        Uri uri = Uri.parse(data.getDataString());

        try {
            PdfReader reader = new PdfReader(getActivity().getContentResolver().openInputStream(uri));
            //File directory = Environment.getExternalStorageDirectory();
            // The generated PDF is in the home directory of the app
            File directory = getActivity().getFilesDir();
            String fileName = getFileName(getActivity(), uri);
            File file = new File(directory, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            PdfWriter writer = new PdfWriter(outputStream);

            // Creating a PdfDocument objects
            PdfDocument srcPdf = new PdfDocument(reader);
            PdfDocument destpdf = new PdfDocument(writer);

            // Opening a page from the existing PDF
            boolean landscape=binding.landscape.isChecked();
            //PageSize nUpPageSize = PageSize.A4;
            PageSize nUpPageSize = srcPdf.getDefaultPageSize();
            if (landscape)
                nUpPageSize = srcPdf.getDefaultPageSize().rotate();
            PdfCanvas canvas;

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

                float scaleWidth=nUpPageSize.getWidth()/(orig.getWidth()*(float)nbColumns);
                float scaleHeight=nUpPageSize.getHeight()/(orig.getHeight()*(float)nbRows);
                float scale=scaleWidth;
                if (scale>scaleHeight)
                    scale=scaleHeight;
                AffineTransform transformationMatrix = AffineTransform.getScaleInstance(scale,scale);
                canvas.concatMatrix(transformationMatrix);

                // Get the next 8 pages from source file
                PdfFormXObject[] pageCopy =new PdfFormXObject[nbRows*nbColumns];
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
                float origHeight=(orig.getHeight()*scale)/scale;
                float offsetHeight=(rowHeight-origHeight)/2f;

                float columnWidth=nUpPageSize.getWidth()/nbColumns/scale;
                float origWidth=(orig.getWidth()*scale)/scale;
                float offsetWidth=(columnWidth-origWidth)/2f;
                for (int row=0;row<nbRows;row++)
                    for (int column=0;column<nbColumns;column++) {
                        if (pageCopy[row*nbColumns+column] != null)
                            canvas.addXObjectAt(pageCopy[row*nbColumns+column],
                                    column * columnWidth + offsetWidth,
                                    (nbRows-row-1) * rowHeight + offsetHeight);
                    }
            }

            // closing the documents
            destpdf.close();
            srcPdf.close();

            // Printing the new PDF
            printPDF(fileName,landscape);

            // Delete the file
            File fdelete = new File(getActivity().getFilesDir(), fileName);
            if (fdelete.exists())
                fdelete.delete();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error in opening and reading the PDF file", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void printPDF(String fileName,boolean landscape){
        PrintManager printManager=(PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
        try
        {
            File directory = getActivity().getFilesDir();
            File file = new File(directory, fileName);
            FileInputStream inputStream = new FileInputStream(file);
            PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(getContext(), inputStream, fileName);
            PrintAttributes attrib = new PrintAttributes.Builder().build();
            if (landscape)
                attrib = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_LANDSCAPE)
                    . build();
            printManager.print(fileName, printAdapter, attrib);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @SuppressLint("Range")
    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf(File.separator);
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}