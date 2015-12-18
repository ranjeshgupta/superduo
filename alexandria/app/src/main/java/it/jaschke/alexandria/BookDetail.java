package it.jaschke.alexandria;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import it.jaschke.alexandria.data.AlexandriaContract;
import it.jaschke.alexandria.services.BookService;


public class BookDetail extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String EAN_KEY = "EAN";
    public static final String TITLE_KEY = "BOOKTITLE";
    private final int LOADER_ID = 10;
    private View rootView;
    private String mEan;
    private String mBookTitle = "";
    private ShareActionProvider mShareActionProvider;

    public BookDetail(){
    }

    public interface Callbacks {
        void toggleToolbarDrawerIndicator(boolean backToHome);
        void onNavigationDrawerItemSelected(int position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mEan = arguments.getString(BookDetail.EAN_KEY);
            mBookTitle = arguments.getString(BookDetail.TITLE_KEY);
            restartLoader();
        }

        rootView = inflater.inflate(R.layout.fragment_full_book, container, false);
        rootView.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent bookIntent = new Intent(getActivity(), BookService.class);
                bookIntent.putExtra(BookService.EAN, mEan);
                bookIntent.setAction(BookService.DELETE_BOOK);
                getActivity().startService(bookIntent);

                // reset the drawer icon
                ((Callbacks) getActivity()).toggleToolbarDrawerIndicator(false);

                // tablet in landscape mode
                if (MainActivity.IS_TABLET && getActivity().findViewById(R.id.right_container) != null) {
                    ((Callbacks) getActivity()).onNavigationDrawerItemSelected(MainActivity.LISTOFBOOKS_FRAGMENT_POS);
                } else {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        // set the title
        getActivity().setTitle(R.string.detail);

        //on rotate
        if (savedInstanceState != null) {
            ((Callbacks) getActivity()).toggleToolbarDrawerIndicator(true);
        }

        return rootView;
    }

    private void restartLoader(){
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.book_detail, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
        setShareBookIntent();
    }

    private void setShareBookIntent() {
        // set the share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.share_text) + " " + mBookTitle + "\r\n" +
                        getString(R.string.share_url) + mEan
        );

        mShareActionProvider.setShareIntent(shareIntent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                getActivity(),
                AlexandriaContract.BookEntry.buildFullBookUri(Long.parseLong(mEan)),
                null,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            return;
        }

        mBookTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.TITLE));
        TextView tvFullBookTitle = (TextView) rootView.findViewById(R.id.fullBookTitle);
        tvFullBookTitle.setText(mBookTitle);
        tvFullBookTitle.setContentDescription(mBookTitle);

        String bookSubTitle = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.SUBTITLE));
        TextView tvFullBookSubTitle = (TextView) rootView.findViewById(R.id.fullBookSubTitle);
        tvFullBookSubTitle.setText(bookSubTitle);
        tvFullBookSubTitle.setContentDescription(bookSubTitle);

        String desc = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.DESC));
        TextView tvFullBookDesc = (TextView) rootView.findViewById(R.id.fullBookDesc);
        tvFullBookDesc.setText(desc);

        String authors = data.getString(data.getColumnIndex(AlexandriaContract.AuthorEntry.AUTHOR));
        TextView tvAuthor = (TextView) rootView.findViewById(R.id.authors);
        if(null == authors){
            tvAuthor.setVisibility(View.GONE);
        }
        else{
            String[] authorsArr = authors.split(",");
            tvAuthor.setLines(authorsArr.length);
            tvAuthor.setText(authors.replace(",", "\n"));
            tvAuthor.setContentDescription(authors);
        }

        String imgUrl = data.getString(data.getColumnIndex(AlexandriaContract.BookEntry.IMAGE_URL));
        ImageView ivFullBookCover = (ImageView) rootView.findViewById(R.id.fullBookCover);
        if(Patterns.WEB_URL.matcher(imgUrl).matches()){
            Picasso.with(getContext())
                    .load(imgUrl)
                    .placeholder(R.drawable.ic_no_image) // optional
                    .error(R.drawable.ic_no_image)    // optional
                    .into(ivFullBookCover);

        }
        else {
            ivFullBookCover.setImageResource(R.drawable.ic_no_image);
        }
        ivFullBookCover.setVisibility(View.VISIBLE);
        ivFullBookCover.setContentDescription(mBookTitle);

        String categories = data.getString(data.getColumnIndex(AlexandriaContract.CategoryEntry.CATEGORY));
        TextView tvCategories = (TextView) rootView.findViewById(R.id.categories);
        tvCategories.setText(categories);
        tvCategories.setContentDescription(categories);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onPause() {
        super.onPause();

        // tablet portrait view
        if (MainActivity.IS_TABLET && getActivity().findViewById(R.id.right_container) == null) {
            getActivity().getSupportFragmentManager().popBackStack(getString(R.string.detail), FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }
}