package com.pyxistech.android.rabbitreminder.activities;

import android.app.ListActivity;
import android.app.AlertDialog.Builder;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.pyxistech.android.rabbitreminder.R;
import com.pyxistech.android.rabbitreminder.adaptaters.TaskListAdapter;
import com.pyxistech.android.rabbitreminder.models.TaskItem;
import com.pyxistech.android.rabbitreminder.models.TaskList;

public class TaskListActivity extends ListActivity {
	
    private static final String[] PROJECTION = new String[] {
        com.pyxistech.android.rabbitreminder.providers.TaskList.Items._ID, // 0
        com.pyxistech.android.rabbitreminder.providers.TaskList.Items.NAME, // 1
        com.pyxistech.android.rabbitreminder.providers.TaskList.Items.DONE, // 1
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

    	TaskListAdapter adapter = new TaskListAdapter(this, new TaskList());

		refreshList(adapter);

    	setListAdapter(adapter);
    	registerForContextMenu(getListView());
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	
    	TaskList list = getTaskListAdapter().getList();
		outState.putParcelable("TaskList", list);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(Menu.NONE, ADD_ITEM, Menu.NONE, R.string.add_item_menu_text);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case ADD_ITEM:
    		addItem();
    		return true;
    	}
    	
    	return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	menu.add(Menu.NONE, EDIT_ITEM, Menu.NONE,  R.string.edit_item_menu_text);
    	menu.add(Menu.NONE, DELETE_ITEM, Menu.NONE, R.string.delete_item_menu_text);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    	switch (item.getItemId()) {
    	case EDIT_ITEM:
    		editItem(info.id);
    		return true;
    	case DELETE_ITEM:
    		deleteItem(info.id);
    		return true;
    	default:
    		return super.onContextItemSelected(item);
    	}
    }
    
    private void editItem(final long index) {
		Intent intent = new Intent(this, AddTaskActivity.class);
		intent.putExtra("index", (int) index);
		intent.putExtra("item", getTaskListAdapter().getItem((int) index)); 
		startActivityForResult(intent, 0);
    }
    
    private void deleteItem(final long index) {
    	Builder builder = new Builder(this);
    	builder.setIcon(android.R.drawable.ic_dialog_alert);
    	builder.setTitle(R.string.delete_item_confirmation_dialog_title);
    	builder.setMessage(R.string.delete_item_confirmation_dialog_text);
    	builder.setPositiveButton(R.string.validation_button_text, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				getContentResolver().delete(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.CONTENT_URI, 
						com.pyxistech.android.rabbitreminder.providers.TaskList.Items._ID + "=" + getTaskListAdapter().getItem((int)index).getIndex(), 
						null);
				getTaskListAdapter().deleteItem((int) index);	
			}
		});
    	builder.setNegativeButton(R.string.cancel_button_text, null);
    	builder.show();
    }
    
    private void addItem() {
		Intent intent = new Intent(this, AddTaskActivity.class);
		startActivityForResult(intent, 0);
	}
    
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if( intent != null ){
	    	String data = intent.getExtras().get("newTaskText").toString();
	    	int index = intent.getExtras().getInt("index");
	    	if (index == -1)
	    	{
	    		getTaskListAdapter().addItem(new TaskItem(data, false));

		    	ContentValues values = new ContentValues();
		    	values.put(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.NAME, data);
		    	values.put(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.DONE, 0);
		    	getContentResolver().insert(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.CONTENT_URI, values);
		    	
		    	refreshList((TaskListAdapter)getListAdapter());
	    	}
	    	else
	    	{
	    		getTaskListAdapter().updateItem(index, data);

		    	ContentValues values = new ContentValues();
		    	values.put(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.NAME, data);
		    	values.put(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.DONE, getTaskListAdapter().getItem(index).isDone() ? 1 : 0);
		    	getContentResolver().update(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.CONTENT_URI, 
		    			values,
		    			com.pyxistech.android.rabbitreminder.providers.TaskList.Items._ID + "=" + getTaskListAdapter().getItem(index).getIndex(), 
		    			null);
		    	
		    	refreshList((TaskListAdapter)getListAdapter());
	    	}
    	}
    }

	@Override
    public void onListItemClick(ListView parent, View v, int position, long id) {
    	CheckedTextView checkableItem = (CheckedTextView) v.findViewById(R.id.task_item);
    	checkableItem.toggle();
    	getModel(position).setDone(checkableItem.isChecked());
    	
    	ContentValues values = new ContentValues();
    	values.put(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.DONE, checkableItem.isChecked() ? 1 : 0);
    	getContentResolver().update(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.CONTENT_URI, 
    			values, com.pyxistech.android.rabbitreminder.providers.TaskList.Items._ID + "=" + getTaskListAdapter().getItem((int)position).getIndex(), 
				null);
    }
    
    private TaskItem getModel(int position) {
    	return getTaskListAdapter().getItem(position);
    }

	private TaskListAdapter getTaskListAdapter() {
		return ((TaskListAdapter) getListAdapter());
	}
     
    public TaskList refreshList(TaskListAdapter adapter) {
        Cursor cursor = managedQuery(com.pyxistech.android.rabbitreminder.providers.TaskList.Items.CONTENT_URI, 
        		PROJECTION, null, null, 
        		com.pyxistech.android.rabbitreminder.providers.TaskList.Items.DEFAULT_SORT_ORDER);
        
        adapter.clearList();
        
        int count = cursor.getCount();
		for (int i = 0; i < count; i++) {
			cursor.moveToPosition(i);
        	adapter.addItem(new TaskItem(Integer.valueOf(cursor.getString(0)), cursor.getString(1), cursor.getInt(2) == 1));
        }
    	
    	return adapter.getList();
    }
    
    public static final int ADD_ITEM = Menu.FIRST + 1;
    public static final int EDIT_ITEM = Menu.FIRST + 2;
    public static final int DELETE_ITEM = Menu.FIRST + 3;
}