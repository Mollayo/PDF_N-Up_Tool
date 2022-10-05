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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.itextpdf.kernel.colors.ColorConstants;
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
import java.io.InputStream;

import me.lebob.pdfmultiplepagespersheetconverter.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private boolean nbColumnsValid=true,nbRowsValid=true,marginValid=true;
    // File name of the PDF to be used for the N-Up
    String fileName;
    // Time stamp to differentiate the PDF files (in case of multiple runs of the app)
    String timeStamp;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        ActivityResultLauncher<Intent> selectPDFActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        selectPDF(data);
                    }
                });

        binding.numberOfColumns.setText("2");
        binding.numberOfColumns.addTextChangedListener(new TextWatcher()  {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                try
                {
                    int val=Integer.parseInt(binding.numberOfColumns.getText().toString());
                    nbColumnsValid=(val>=1 && val<=100);
                }
                catch (Exception e)
                {
                    nbColumnsValid=false;
                }
                if (nbColumnsValid)
                    binding.numberOfColumns.setError(null);
                else
                    binding.numberOfColumns.setError("Enter a value between 1 and 100");
                setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        binding.numberOfRows.setText("3");
        binding.numberOfRows.addTextChangedListener(new TextWatcher()  {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                try
                {
                    int val=Integer.parseInt(binding.numberOfRows.getText().toString());
                    nbRowsValid=(val>=1 && val<=100);
                }
                catch (Exception e)
                {
                    nbRowsValid=false;
                }
                if (nbRowsValid)
                    binding.numberOfRows.setError(null);
                else
                    binding.numberOfRows.setError("Enter a value between 1 and 100");
                setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        binding.margin.addTextChangedListener(new TextWatcher()  {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable editable) {
                try
                {
                    int val=Integer.parseInt(binding.margin.getText().toString());
                    marginValid=(val>=0 && val<=100);
                }
                catch (Exception e)
                {
                    marginValid=false;
                }
                if (marginValid)
                    binding.margin.setError(null);
                else
                    binding.margin.setError("Enter a value between 0 and 100");
                setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });
        binding.selectPdf.setOnClickListener(view1 -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            selectPDFActivityResultLauncher.launch(intent);
        });

        setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
        binding.printPdf.setOnClickListener(view1 -> {
            generatePDF();
        });
        onSharedIntent();
    }

    private void setPrintButtonState(boolean enabled)
    {
        if (enabled)
            binding.printPdf.setAlpha(1f);
        else
            binding.printPdf.setAlpha(.5f);
        binding.printPdf.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        // Delete all the files in the cache
        if (fileName!=null)
        {
            File directory = requireActivity().getCacheDir();
            File fdelete = new File(directory, timeStamp+fileName);
            if (fdelete.exists())
                fdelete.delete();
        }
        fileName=null;
        timeStamp=null;
        super.onDestroyView();
        binding = null;
    }

    public void generatePDF() {
        try {
            File directory = getActivity().getCacheDir();
            PdfReader reader = new PdfReader(new File(directory, timeStamp+fileName));
            // The generated PDF is in the home directory of the app
            File file = new File(directory, fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            PdfWriter writer = new PdfWriter(outputStream);

            // Creating a PdfDocument objects
            PdfDocument srcPdf = new PdfDocument(reader);
            PdfDocument destpdf = new PdfDocument(writer);

            // Opening a page from the existing PDF
            boolean landscape=binding.landscape.isChecked();
            boolean drawframe=binding.drawframe.isChecked();
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

                // 1 inch: 72 user units
                // 1 cm: 72/2.54 user units
                // 1 user unit : 2.54/72

                float nUpPageWidth= nUpPageSize.getWidth();
                float nUpPageHeight=nUpPageSize.getHeight();
                float margin=Integer.parseInt(binding.margin.getText().toString())/10f;
                float marginWidth=margin*(72f/2.54f)*2f*((float)nbColumns);
                float marginHeight=margin*(72f/2.54f)*2f*((float)nbRows);
                float nUpPageWidthWithMargin= nUpPageSize.getWidth()-marginWidth;
                float nUpPageHeightWithMargin=nUpPageSize.getHeight()-marginHeight;
                float origWidth=orig.getWidth();
                float origHeight=orig.getHeight();

                float scaleWidth=nUpPageWidthWithMargin/(origWidth*(float)nbColumns);
                float scaleHeight=nUpPageHeightWithMargin/(origHeight*(float)nbRows);
                float scale=scaleWidth;
                if (scale>scaleHeight)
                    scale=scaleHeight;
                AffineTransform transformationMatrix = AffineTransform.getScaleInstance(scale,scale);
                canvas.concatMatrix(transformationMatrix);

                // Get the next pages from source file
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

                float rowHeight=nUpPageHeight/nbRows/scale;
                float origHeightInNUp=(orig.getHeight()*scale)/scale;
                float offsetHeight=(rowHeight-origHeightInNUp)/2f;

                float columnWidth=nUpPageWidth/nbColumns/scale;
                float origWidthInNUp=(orig.getWidth()*scale)/scale;
                float offsetWidth=(columnWidth-origWidthInNUp)/2f;
                for (int row=0;row<nbRows;row++)
                    for (int column=0;column<nbColumns;column++) {
                        if (pageCopy[row*nbColumns+column] != null) {
                            canvas.addXObjectAt(pageCopy[row * nbColumns + column],
                                    column * columnWidth + offsetWidth,
                                    (nbRows - row - 1) * rowHeight + offsetHeight);
                            canvas.setColor(ColorConstants.BLACK,false);
                            // Draw the frame for each copy
                            if (drawframe)
                                canvas.setStrokeColor(ColorConstants.BLACK).setLineWidth(1).rectangle(
                                        column * columnWidth + offsetWidth,
                                        (nbRows - row - 1) * rowHeight + offsetHeight,
                                        origWidth,
                                        origHeight).stroke();
                        }
                    }
            }

            // closing the documents
            destpdf.close();
            srcPdf.close();

            // Printing the new PDF
            printPDF(fileName,landscape);

            // Delete the new PDF file
            File fdelete = new File(getActivity().getCacheDir(), fileName);
            if (fdelete.exists())
                fdelete.delete();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error printing the PDF file", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void selectPDF(Intent data)
    {
        // get the new value from Intent data
        Uri uri = Uri.parse(data.getDataString());
        // Copy the file in the cache directory
        copyPDF(uri);
    }

    private void printPDF(String fileName,boolean landscape){
        PrintManager printManager=(PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);
        try
        {
            File directory = getActivity().getCacheDir();
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

    private void copyPDF(Uri uri) {
        // Copy the file in the cache directory
        final int EOF = -1;
        final int DEFAULT_BUFFER_SIZE = 1024 * 4;
        try {
            fileName = getFileName(getActivity(), uri);

            // Open the PDF file to check its validity
            InputStream inputStream = requireActivity().getContentResolver().openInputStream(uri);
            PdfReader reader = new PdfReader(inputStream);
            PdfDocument srcPdf = new PdfDocument(reader);
            int nbPages=srcPdf.getNumberOfPages();
            srcPdf.close();

            inputStream = requireActivity().getContentResolver().openInputStream(uri);
            // Generate a timestamp to identify the file
            timeStamp=String.valueOf(android.os.SystemClock.elapsedRealtime());
            File file = new File(requireActivity().getCacheDir(), timeStamp+fileName);
            FileOutputStream outputStream = new FileOutputStream(file);

            long count = 0;
            int n;
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            while (EOF != (n = inputStream.read(buffer))) {
                outputStream.write(buffer, 0, n);
                count += n;
            }
            outputStream.close();
            binding.filename.setText(fileName);
            setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error opening and reading the PDF file", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            fileName=null;
            timeStamp=null;
            binding.filename.setText("");
        }
        setPrintButtonState(nbColumnsValid&&nbRowsValid&&marginValid&&fileName!=null);
    }

    private void onSharedIntent() {
        Intent receivedIntent = getActivity().getIntent();
        String receivedAction = receivedIntent.getAction();
        String receivedType = receivedIntent.getType();

        if (receivedAction.equals(Intent.ACTION_SEND)) {
            if (receivedType.startsWith("application/pdf")) {
                Uri receiveUri = (Uri) receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (receiveUri != null) {
                    // A PDF file has been shared with this app
                    copyPDF(receiveUri);
                }
            }
        }
        else if (receivedAction.equals(Intent.ACTION_VIEW)) {
            if (receivedType.startsWith("application/pdf")) {
                Uri receiveUri = Uri.parse(receivedIntent.getDataString());
                if (receiveUri != null) {
                    // A PDF file has been sent to this app
                    copyPDF(receiveUri);
                }
            }
        }
    }
}