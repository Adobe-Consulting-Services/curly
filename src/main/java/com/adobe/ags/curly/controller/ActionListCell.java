/* 
 * Copyright 2015 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ags.curly.controller;

import com.adobe.ags.curly.CurlyApp;
import com.adobe.ags.curly.model.ActionUtils;
import com.adobe.ags.curly.xml.Action;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class ActionListCell extends ListCell<Action> {
    static DataFormat actionDataFormat = new DataFormat("action");

    public ActionListCell() {
        setOnDragDetected((javafx.scene.input.MouseEvent event) -> {
            if (getItem() == null) {
                return;
            }
            getListView().getSelectionModel().clearSelection();
            Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(actionDataFormat, getItem());
            dragboard.setDragView(snapshot(null, new WritableImage(75, 24)));
            dragboard.setContent(content);
            event.consume();
        });
        setOnDragOver((javafx.scene.input.DragEvent event) -> {
            if (event.getGestureSource() != this && event.getDragboard().hasContent(actionDataFormat)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });
        setOnDragEntered((event) -> {
            if (event.getGestureSource() != this && event.getDragboard().hasContent(actionDataFormat)) {
                setOpacity(0.3);
                setStyle("-fx-border-color:red; -fx-border-width: 2 0 0 0;");
            }
        });
        setOnDragExited((event) -> {
            if (event.getGestureSource() != this && event.getDragboard().hasContent(actionDataFormat)) {
                setOpacity(1);
                setStyle("-fx-border-width: 0;");
            }
        });
        setOnDragDropped((DragEvent event) -> {
            if (getItem() == null) {
                return;
            }
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(actionDataFormat)) {
                ObservableList<Action> items = getListView().getItems();
                Action draggedContent = ((ListCell<Action>) event.getGestureSource()).getItem();
                Action droppedContent = ((ListCell<Action>) event.getGestureTarget()).getItem();
                int sourceIdx = items.indexOf(draggedContent);
                int targetIdx = items.indexOf(droppedContent);
                items.add(targetIdx, draggedContent);
                items.remove(sourceIdx > targetIdx ? sourceIdx + 1 : sourceIdx);
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });
        setOnDragDone(DragEvent::consume);
    }

    @Override
    protected void updateItem(Action item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText("");
        } else {
            setText(item.getName());
            if (item.getDescription() != null && !(item.getDescription().isEmpty())) {
                setTooltip(new Tooltip(item.getDescription()));
            }
            if (ActionUtils.isFavorite(item)) {
                setGraphic(getFavoriteIcon());
            } else {
                setGraphic(null);
            }
        }
    }

    @Override
    public void startEdit() {
        CurlyApp.editAction(getItem(), null);
        cancelEdit();
        updateItem(getItem(), false);
        Action dummy = new Action();
        getListView().getItems().add(dummy);
        getListView().getItems().remove(dummy);
    }

    static Image favoriteIcon = new Image(ActionListCell.class.getClassLoader().getResourceAsStream("images/favorite.png"));
    private ImageView getFavoriteIcon() {
        return new ImageView(favoriteIcon);
    }
}
