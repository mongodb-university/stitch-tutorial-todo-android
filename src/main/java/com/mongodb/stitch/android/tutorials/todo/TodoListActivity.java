/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.android.tutorials.todo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoClient;
import com.mongodb.stitch.android.services.mongodb.remote.RemoteMongoCollection;
import com.mongodb.stitch.core.internal.common.BsonUtils;
import java.util.ArrayList;
import java.util.List;
import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.types.ObjectId;

public class TodoListActivity extends AppCompatActivity {
    private static final String TAG = TodoListActivity.class.getSimpleName();
    private TodoAdapter todoAdapter;
    private RemoteMongoCollection<TodoItem> items;
    private String userId;

    public static StitchAppClient client;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        // TODO:
        // 1. Instantiate the Stitch client
        // client =

        // 2. Instantiate a RemoteMongoClient
        // final RemoteMongoClient mongoClient =

        // 3. Set up the items collection
        // items =

        // Set up recycler view for to-do items
        final RecyclerView todoRecyclerView = findViewById(R.id.rv_todo_items);
        final RecyclerView.LayoutManager todoLayoutManager = new LinearLayoutManager(this);
        todoRecyclerView.setLayoutManager(todoLayoutManager);

        // Set up adapter
        todoAdapter = new TodoAdapter(
                new ArrayList<>(),
                new TodoAdapter.ItemUpdater() {
                    @Override
                    public void updateChecked(final ObjectId itemId, final boolean isChecked) {
                        final Document updateDoc =
                                new Document("$set", new Document(TodoItem.Fields.CHECKED, isChecked));
                        items.updateOne(new Document("_id", itemId), updateDoc);
                    }

                    @Override
                    public void updateTask(final ObjectId itemId, final String currentTask) {
                        showEditItemDialog(itemId, currentTask);
                    }
                });
        todoRecyclerView.setAdapter(todoAdapter);
        doLogin();
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        // Only enable options besides login when logged in
        final boolean loggedIn = Stitch.getDefaultAppClient().getAuth().isLoggedIn();
        for (int i = 0; i < menu.size(); i++) {
            if (menu.getItem(i).getItemId() == R.id.login_action) {
                menu.getItem(i).setTitle(loggedIn ? "Log out" : "Log in");
                continue;
            }
            menu.getItem(i).setEnabled(loggedIn);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.todo_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_action:
                if (item.getTitle() == "Log out" && client != null && client.getAuth().getUser().isLoggedIn()) {
                    client.getAuth().logout().addOnSuccessListener(v -> {
                        doLogin();
                    });
                } else doLogin();
                return true;
            case R.id.add_todo_item_action:
                showAddItemDialog();
                return true;
            case R.id.clear_checked_action:
                clearCheckedTodoItems();
                return true;
            case R.id.clear_all_action:
                clearAllTodoItems();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void doLogin() {
        if (client.getAuth().getUser() != null && client.getAuth().getUser().isLoggedIn()) {
            userId = client.getAuth().getUser().getId();
            TextView tvId = findViewById(R.id.txt_user_id);
            tvId.setText("Logged in with ID \"" + userId + "\"");
            todoAdapter.refreshItemList(getItems());
            return;
        } else {
            Intent intent = new Intent(TodoListActivity.this, LogonActivity.class);
            startActivityForResult(intent, 111);
        }
    }

    private void showAddItemDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Item");

        final View view = getLayoutInflater().inflate(R.layout.edit_item_dialog, null);
        final EditText input = view.findViewById(R.id.et_todo_item_task);

        builder.setView(view);

        // Set up the buttons
        builder.setPositiveButton(
                "Add",
                (dialog, which) -> addTodoItem(input.getText().toString()));
        builder.setNegativeButton(
                "Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showEditItemDialog(final ObjectId itemId, final String currentTask) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item");

        final View view = getLayoutInflater().inflate(R.layout.edit_item_dialog, null);
        final EditText input = view.findViewById(R.id.et_todo_item_task);

        input.setText(currentTask);
        input.setSelection(input.getText().length());

        builder.setView(view);

        // Set up the buttons
        builder.setPositiveButton(
                "Update",
                (dialog, which) -> updateTodoItemTask(itemId, input.getText().toString()));
        builder.setNegativeButton(
                "Cancel",
                (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private Task<List<TodoItem>> getItems() {
        return items.find().into(new ArrayList<>());
    }

    private void addTodoItem(final String task) {
        final TodoItem newItem = new TodoItem(new ObjectId(), userId, task, false);
        items.insertOne(newItem)
                .addOnSuccessListener(result -> {
                    todoAdapter.addItem(newItem);
                })
                .addOnFailureListener(e -> Log.e(TAG, "failed to insert todo item", e));
    }

    private void updateTodoItemTask(final ObjectId itemId, final String newTask) {
        final BsonObjectId docId = new BsonObjectId(itemId);
        items.updateOne(
                new Document("_id", docId),
                new Document("$set", new Document(TodoItem.Fields.TASK, newTask)))
                .addOnSuccessListener(result -> {
                    items.find(new Document("_id", docId)).first()
                            .addOnSuccessListener(item -> {
                                if (item == null) {
                                    return;
                                }
                                todoAdapter.updateItem(item);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "failed to find todo item", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "failed to insert todo item", e));
    }

    private void clearCheckedTodoItems() {
        getItems().addOnSuccessListener(todoItems -> {
            for (final TodoItem item : todoItems) {
                if (item.isChecked()) {
                    items.deleteOne(new Document("_id", item.get_id()));
                }
            }
            todoAdapter.refreshItemList(getItems());
        });
    }

    private void clearAllTodoItems() {
        getItems().addOnSuccessListener(todoItems -> {
            for (final TodoItem item : todoItems) {
                items.deleteOne(new Document("_id", item.get_id())).addOnSuccessListener(remoteDeleteResult -> {
                    //tasks.add();
                });
            }
            todoAdapter.refreshItemList(getItems());
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == 111) {
            doLogin();
        }
    }
}
