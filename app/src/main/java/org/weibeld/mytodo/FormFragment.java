package org.weibeld.mytodo;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.weibeld.mytodo.data.TodoDatabaseHelper;
import org.weibeld.mytodo.data.TodoItem;
import org.weibeld.mytodo.util.MyDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

import static android.app.Activity.RESULT_OK;
import static nl.qbusict.cupboard.CupboardFactory.cupboard;
import static org.weibeld.mytodo.R.array.date;

/**
 * Created by dw on 06/02/17.
 */

public class FormFragment extends Fragment {

    private final String LOG_TAG = FormFragment.class.getSimpleName();

    SQLiteDatabase mDb;

    Spinner mSpinPrior;
    ArrayAdapter<CharSequence> mSpinPriorAdapter;
    Spinner mSpinDate;
    ArrayAdapter<CharSequence> mSpinDateAdapter;
    ArrayList<String> mSpinDateItems;
    EditText mEditText;
    Button mButton;

    MainActivity mMainActivity;
    EditActivity mEditActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_form, container, false);

        mMainActivity = null;
        mEditActivity = null;
        Activity activity = getActivity();
        if      (activity instanceof MainActivity) mMainActivity = (MainActivity) activity;
        else if (activity instanceof EditActivity) mEditActivity = (EditActivity) activity;

        mDb = (new TodoDatabaseHelper(getActivity())).getWritableDatabase();
        mEditText = (EditText) rootView.findViewById(R.id.etNewItem);
        mSpinPrior = (Spinner) rootView.findViewById(R.id.spinPriority);
        mSpinDate = (Spinner) rootView.findViewById(R.id.spinDate);
        mButton = (Button) rootView.findViewById(R.id.button);

        // Priority spinner (read selected value in onAdditem method)
        mSpinPriorAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.priority, android.R.layout.simple_spinner_item);
        mSpinPriorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinPrior.setAdapter(mSpinPriorAdapter);

        // Date spinner (read selected value in onAdditem method)
        mSpinDateItems = new ArrayList<>(Arrays.asList(getResources().getStringArray(date)));
        mSpinDateAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, mSpinDateItems);
        mSpinDateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinDate.setAdapter(mSpinDateAdapter);
        mSpinDate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // "None" selected: remove the dynamically added date item if present
                if (position == 0) {
                    if (mSpinDateItems.size() > 2) {
                        mSpinDateItems.remove(2);
                        mSpinDateAdapter.notifyDataSetChanged();
                    }
                }
                // "Select..." selected: launch the date picker dialog
                if (position == 1) {
                    DialogFragment datePicker = new DatePickerFragment();
                    datePicker.show(getFragmentManager(), "datePicker");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEditText.length() == 0) {
                    Toast.makeText(getActivity(), R.string.toast_enter_text, Toast.LENGTH_SHORT).show();
                    return;
                }
                TodoItem item = null;
                if      (mMainActivity != null) item = new TodoItem();
                else if (mEditActivity != null) item = mEditActivity.mItem;

                // Set the item properties according to the input form fields
                item.text = mEditText.getText().toString();
                item.priority = mSpinPrior.getSelectedItemPosition();
                if (mSpinDate.getSelectedItemPosition() == 2)
                    item.due_ts = new MyDate((String) mSpinDate.getSelectedItem()).getTimestamp();
                else
                    item.due_ts = null;

                if (mMainActivity != null) {
                    // Add the new item to the database and to the ArrayList
                    cupboard().withDatabase(mDb).put(item);
                    mMainActivity.mItemsAdapter.add(item);
                    mMainActivity.mItemsAdapter.notifyDataSetChanged();
                    // Reset input fields
                    mEditText.setText("");
                    mSpinPrior.setSelection(0);
                    if (mSpinDateItems.size() > 2) {
                        mSpinDateItems.remove(2);
                        mSpinDateAdapter.notifyDataSetChanged();
                    }
                    mSpinDate.setSelection(0);
                    // Scroll to end of list
                    mMainActivity.mListView.setSelection(mMainActivity.mItemsAdapter.getCount() - 1);
                }
                else if (mEditActivity != null) {
                    Intent result = new Intent();
                    result.putExtra(MainActivity.EXTRA_CODE_ITEM, item);
                    result.putExtra(MainActivity.EXTRA_CODE_ITEM_POS, mEditActivity.mPosition);
                    mEditActivity.setResult(RESULT_OK, result);
                    mEditActivity.finish();
                }
            }
        });

        // Customise look and feel
        if (mMainActivity != null) {
            rootView.findViewById(R.id.form_container).setBackgroundColor(Color.parseColor("#FFF9C4"));
            mButton.setText(R.string.button_add);
            mEditText.setHint(R.string.hint_new);
        }
        else if (mEditActivity != null) {
            rootView.findViewById(R.id.form_container).setBackgroundColor(Color.TRANSPARENT);
            mButton.setText(R.string.button_save);
            mEditText.setHint(R.string.hint_edit);
        }

        // If attached to EditActivity, get the item to edit and set the input fields accordingly
        if (mEditActivity != null) {
            TodoItem item = mEditActivity.mItem;
            mEditText.setText(item.text);
            mEditText.setSelection(mEditText.getText().length());  // Set cursor to end of text
            mSpinPrior.setSelection(item.priority);
            if (item.due_ts == null)
                mSpinDate.setSelection(0);
            else {
                mSpinDateItems.add(getDueDateSpinnerString(new MyDate(item.due_ts)));
                mSpinDateAdapter.notifyDataSetChanged();
                mSpinDate.setSelection(2);
            }
        }

        return rootView;
    }

    public static String getDueDateSpinnerString(MyDate date) {
        return "Due " + date.toString();
    }

    /**
     * Date picker dialog.
     * Created by dw on 27/01/17.
     */
    // TODO: improve handling of dates (new class providing format and parse methods)
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private final String LOG_TAG = DatePickerFragment.class.getSimpleName();

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            // TODO: if a date has been previously selected, set this date as the default date
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        // Called if the user selected a date from the date picker dialog
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
            MyDate date = new MyDate(year, month, dayOfMonth);
            FormFragment f = (FormFragment) getActivity().getFragmentManager().findFragmentById(R.id.fragment_container);
            // If a date has been selected before, replace it in the spinner, else add the new date
            if (f.mSpinDateItems.size() > 2)
                f.mSpinDateItems.set(2, getDueDateSpinnerString(date));
            else
                f.mSpinDateItems.add(getDueDateSpinnerString(date));
            f.mSpinDateAdapter.notifyDataSetChanged();
            f.mSpinDate.setSelection(2);
        }

        // Called if the user cancelled the date picker dialog
        @Override
        public void onCancel(DialogInterface dialog) {
            FormFragment f = (FormFragment) getActivity().getFragmentManager().findFragmentById(R.id.fragment_container);
            // Set the selected item away from the currently selected "Select..." item
            if (f.mSpinDateItems.size() > 2)
                f.mSpinDate.setSelection(2);
            else
                f.mSpinDate.setSelection(0);
        }
    }
}