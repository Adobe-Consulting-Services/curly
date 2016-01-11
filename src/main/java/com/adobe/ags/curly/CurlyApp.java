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
package com.adobe.ags.curly;

import com.adobe.ags.curly.controller.ActionPanelController;
import com.adobe.ags.curly.controller.AppController;
import com.adobe.ags.curly.controller.BatchRunner;
import com.adobe.ags.curly.controller.DataImporterController;
import com.adobe.ags.curly.controller.RunnerActivityController;
import com.adobe.ags.curly.xml.Action;
import com.adobe.ags.curly.xml.ErrorBehavior;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;
import static javafx.application.Application.launch;

public class CurlyApp extends Application {

    static CurlyApp singleton;
    static final String APPLICATION_TITLE = "applicationTitle";
    private AppController appController;
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private final ObjectProperty<ErrorBehavior> errorBehavior = new SimpleObjectProperty<>(ErrorBehavior.IGNORE);
    private ResourceBundle i18n;

    public static CurlyApp getInstance() {
        if (singleton == null) {
            singleton = new CurlyApp();
        }
        return singleton;
    }

    private Stage applicationWindow;

    public BooleanProperty runningProperty() {
        return isRunning;
    }

    public ObjectProperty<ErrorBehavior> errorBehaviorProperty() {
        return errorBehavior;
    }

    public static String getMessage(String key) {
        if (singleton == null || singleton.i18n == null) {
            return key;
        } else {
            return singleton.i18n.getString(key);
        }
    }

    public Action editAction(Action source, Runnable persistHandler) {
        final BooleanProperty okPressed = new SimpleBooleanProperty(false);
        if (source == null) {
            source = new Action();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ActionPanel.fxml"));
            loader.setResources(i18n);
            loader.load();
            ActionPanelController actionController = loader.getController();

            Stage popup = new Stage();
            popup.setScene(new Scene(loader.getRoot()));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(applicationWindow);

            actionController.populateValues(source, true);
            actionController.onPersist(() -> {
                if (persistHandler != null) {
                    persistHandler.run();
                }
                okPressed.set(true);
            });
            actionController.whenFinished(popup::close);

            popup.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(CurlyApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (okPressed.get()) {
            return source;
        } else {
            return null;
        }
    }

    public void importWizard(Consumer<List<Map<String, String>>> handler) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DataImporter.fxml"));
            loader.setResources(i18n);
            loader.load();
            DataImporterController importController = loader.getController();

            Stage popup = new Stage();
            popup.setScene(new Scene(loader.getRoot()));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(applicationWindow);

            importController.setActions(appController.getActions());
            importController.setFinishImportHandler(handler);
            importController.whenFinished(popup::close);

            popup.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(CurlyApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void openActivityMonitor(BatchRunner runner) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/RunnerReport.fxml"));
            loader.setResources(i18n);
            loader.load();
            RunnerActivityController runnerActivityController = loader.getController();

            Stage popup = new Stage();
            popup.setScene(new Scene(loader.getRoot()));
            popup.initModality(Modality.APPLICATION_MODAL);
            popup.initOwner(applicationWindow);

            runnerActivityController.attachRunner(runner);

            popup.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(CurlyApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void start(Stage stage) throws Exception {
        singleton = this;
        applicationWindow = stage;
        Locale locale = Locale.getDefault();
        i18n = ResourceBundle.getBundle("Bundle", locale);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/App.fxml"));
        loader.setResources(i18n);
        loader.load();
        Parent root = loader.getRoot();
        appController = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle(i18n.getString(APPLICATION_TITLE));
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(event -> {
            ConnectionManager.getInstance().shutdown();
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
