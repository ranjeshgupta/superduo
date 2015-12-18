package it.jaschke.alexandria;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.squareup.picasso.Picasso;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;
import it.jaschke.alexandria.barcode.BarcodeCaptureActivity;

public class AddBook extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "INTENT_TO_SCAN_ACTIVITY";
    private EditText mEan;
    private String mEanPrefix = "978";
    private final int LOADER_ID = 1;
    private View rootView;
    private final String EAN_CONTENT="eanContent";
    private static final int RC_BARCODE_CAPTURE = 9001;

    public AddBook(){
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mEan!=null) {
            outState.putString(EAN_CONTENT, mEan.getText().toString());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_add_book, container, false);
        mEan = (EditText) rootView.findViewById(R.id.ean);

        mEan.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                //no need
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //no need
            }

            @Override
            public void afterTextChanged(Editable s) {
                launchAddBook(s.toString().trim());
            }
        });

        // submit on enter
        mEan.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (((event != null) &&
                        (event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) ||
                        (actionId == EditorInfo.IME_ACTION_DONE)) {

                    searchBook(true);
                }

                return true;
            }
        });

        rootView.findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //reset all fields
                clearFields();
                mEan.setText("");

                Context context = getActivity();
                //Toast.makeText(context, "This button should let you scan a book for its barcode!", Toast.LENGTH_SHORT).show();

                // launch barcode activity.
                Intent intent = new Intent(context, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, true);
                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        });

        rootView.findViewById(R.id.save_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEan.setText("");
            }
        });

        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, mEan.getText().toString());
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);
                mEan.setText("");
            }
        });

        if(savedInstanceState!=null){
            mEan.setText(savedInstanceState.getString(EAN_CONTENT));
            mEan.setHint("");
        }

        //Utility.setupUI(rootView, getActivity());
        return rootView;
    }

    private void searchBook(boolean forced) {

        // get the value entered in the searchfield
        String ean = mEan.getText().toString().trim();

        // check if an isbn number was entered
        if ((ean.length() == 10) || (ean.length() == 13)) {

            // prefix isbn10 numbers
            if ((ean.length() == 10) && !ean.startsWith(mEanPrefix)) {
                ean = mEanPrefix + ean;
            }

            // if we have a string of 13 digits
            if (ean.length() == 13) {
                // start a bookservice intent to call the books api
                if (Utility.isNetworkAvailable(getActivity())) {
                    launchAddBook(ean);

                    // restart the loader to populate the preview view
                    restartLoader();
                } else {
                    if (forced) {
                        // show toast when we don't have a network connection
                        Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else if (forced) {
            // show toast when no text was entered
            Toast.makeText(getActivity(), R.string.ean_required, Toast.LENGTH_LONG).show();
        }
    }

    private void launchAddBook(String s){
        String ean = s.toString().trim();
        //catch isbn10 numbers
        if(ean.length()==10 && !ean.startsWith(mEanPrefix)){
            ean = mEanPrefix + ean;
        }
        if(ean.length()<13){
            clearFields();
            return;
        }

        clearFields();
        if (Utility.isNetworkAvailable(getActivity())) {
            Utility.hideSoftKeyboard(getActivity());

            //Once we have an ISBN, start a book intent
            Intent bookIntent = new Intent(getActivity(), BookService.class);
            bookIntent.putExtra(BookService.EAN, ean);
            bookIntent.setAction(BookService.FETCH_BOOK);
            getActivity().startService(bookIntent);
            AddBook.this.restartLoader();
        }
        else{
            Toast.makeText(getActivity(), R.string.no_network, Toast.LENGTH_LONG).show();
        }
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public android.support.v4.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if(mEan.getText().length()==0){
            return null;
        }
        String eanStr= mEan.getText().toString();
        if(eanStr.length()==10 && !eanStr.startsWith(mEanPrefix)){
            eanStr = mEanPrefix + eanStr;
        }
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(eanStr)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        String bookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        TextView tvBookTitle = (TextView) rootView.findViewById(R.id.bookTitle);
        tvBookTitle.setText(bookTitle);
        tvBookTitle.setContentDescription(bookTitle);


        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        TextView tvBookSubTitle = (TextView) rootView.findViewById(R.id.bookSubTitle);
        if(null == bookSubTitle) {
            bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
            if(null != bookSubTitle) {
                bookSubTitle = bookSubTitle.substring(0, 50) + "...";
            }
        }
        if(null == bookSubTitle) {
            tvBookSubTitle.setVisibility(View.GONE);
        }
        else {
            tvBookSubTitle.setText(bookSubTitle);
            tvBookSubTitle.setContentDescription(bookSubTitle);
        }

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        TextView tvAuthor = (TextView) rootView.findViewById(R.id.authors);

        if (authors == null) {
            tvAuthor.setVisibility(View.GONE);
        }
        else {
            String[] authorsArr = authors.split(",");
            tvAuthor.setLines(authorsArr.length);
            tvAuthor.setText(authors.replace(",", "\n"));
            tvAuthor.setContentDescription(authors);
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        ImageView ivBookCover = (ImageView) rootView.findViewById(R.id.bookCover);
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            Picasso.with(getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_no_image) // optional
                    .error(R.drawable.ic_no_image)    // optional
                    .into(ivBookCover);
        }
        else {
            ivBookCover.setImageResource(R.drawable.ic_no_image);
        }
        ivBookCover.setContentDescription(bookTitle);
        ivBookCover.setVisibility(View.VISIBLE);

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        TextView tvCategories = (TextView) rootView.findViewById(R.id.categories);
        if(categories == null){
            tvCategories.setVisibility(View.GONE);
        }
        else{
            tvCategories.setText(categories);
            tvCategories.setContentDescription(categories);
        }

        rootView.findViewById(R.id.save_button).setVisibility(View.VISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<Cursor> loader) {

    }

    private void clearFields(){
        ((TextView) rootView.findViewById(R.id.bookTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.bookSubTitle)).setText("");
        ((TextView) rootView.findViewById(R.id.authors)).setText("");
        ((TextView) rootView.findViewById(R.id.categories)).setText("");
        rootView.findViewById(R.id.bookCover).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.save_button).setVisibility(View.INVISIBLE);
        rootView.findViewById(R.id.delete_button).setVisibility(View.INVISIBLE);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.setTitle(R.string.scan);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    //Toast.makeText(getActivity(), R.string.barcode_success, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Barcode read: " + barcode.displayValue);

                    if(Utility.isInteger(barcode.displayValue, 10)) {
                        mEan.setText(barcode.displayValue);
                        searchBook(true);
                    }
                    else{
                        Toast.makeText(getActivity(), R.string.ean_invalid, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.barcode_failure, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                Toast.makeText(getActivity(), String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)), Toast.LENGTH_LONG).show();
            }
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
