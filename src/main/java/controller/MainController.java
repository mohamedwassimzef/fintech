package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class MainController {

    @FXML
    private Button SubAsset;

    @FXML
    private TextField AssName;

    @FXML
    private TextField AssType;

    @FXML
    private TextField AssValue;

    @FXML
    private TextField AssDesc;

    @FXML
    private DatePicker AssCreated;

    @FXML
    private TextField AssUser;

    @FXML
    private TextField nameField;

    @FXML
    private TableView<InsuredAsset> assetsTable;

    @FXML
    private TableColumn<InsuredAsset, Integer> colId;

    @FXML
    private TableColumn<InsuredAsset, String> colName;

    @FXML
    private TableColumn<InsuredAsset, String> colType;

    @FXML
    private TableColumn<InsuredAsset, Double> colValue;

    @FXML
    private TableColumn<InsuredAsset, String> colDescription;

    @FXML
    private TableColumn<InsuredAsset, LocalDateTime> colCreatedAt;

    @FXML
    private TableColumn<InsuredAsset, Integer> colUserId;

    @FXML
    private Button updateAssetButton;

    @FXML
    private Button deleteAssetButton;

    @FXML
    private TabPane mainTabs;

    @FXML
    private Tab assetFormTab;

    @FXML
    private Tab assetListTab;

    private final ObservableList<InsuredAsset> assets = FXCollections.observableArrayList();
    private InsuredAsset selectedAsset;
    private boolean isEditMode = false;

    @FXML
    public void handleAdd() {
        System.out.println(nameField.getText());
    }

    @FXML
    private void initialize() {
        // Wire table columns to entity properties.
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        colId.setVisible(false);

        setAssetActionButtonsEnabled(false);

        assetsTable.setItems(assets);
        assetsTable.setRowFactory(table -> {
            TableRow<InsuredAsset> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    InsuredAsset item = row.getItem();
                    selectedAsset = item;
                    setAssetActionButtonsEnabled(true);
                    System.out.println("Selected asset id: " + item.getId());
                }
            });
            return row;
        });

        refreshAssetsTable();
        setEditMode(false);
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;
        if (enabled) {
            SubAsset.setText("Update");
        } else {
            SubAsset.setText("Submit");
        }
    }

    private void setAssetActionButtonsEnabled(boolean enabled) {
        updateAssetButton.setDisable(!enabled);
        deleteAssetButton.setDisable(!enabled);
    }

    @FXML
    private void handleUpdateAssetButton() {
        if (selectedAsset == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
            return;
        }

        AssName.setText(selectedAsset.getName());
        AssType.setText(selectedAsset.getType());
        AssValue.setText(String.valueOf(selectedAsset.getValue()));
        AssDesc.setText(selectedAsset.getDescription());
        AssUser.setText(String.valueOf(selectedAsset.getUserId()));

        if (selectedAsset.getCreatedAt() != null) {
            AssCreated.setValue(selectedAsset.getCreatedAt().toLocalDate());
        } else {
            AssCreated.setValue(null);
        }

        setEditMode(true);
        mainTabs.getSelectionModel().select(assetFormTab);
    }

    private void refreshAssetsTable() {
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        List<InsuredAsset> allAssets = assetDAO.readAll();
        assets.setAll(allAssets);
        selectedAsset = null;
        setAssetActionButtonsEnabled(false);
        setEditMode(false);
    }

    @FXML
    private void handleSubmitButton() {
        System.out.println("Submit button clicked!");

        try {
            // Get form values
            String name = AssName.getText().trim();
            String type = AssType.getText().trim();
            String value = AssValue.getText().trim();
            String description = AssDesc.getText().trim();
            String userId = AssUser.getText().trim();

            // Validate required fields
            if (name.isEmpty() || type.isEmpty() || value.isEmpty() || userId.isEmpty()) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "Please fill in all required fields (Name, Type, Value, User ID)");
                return;
            }

            // Parse numeric values
            double assetValue;
            int userIdInt;

            try {
                assetValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "Value must be a valid number");
                return;
            }

            try {
                userIdInt = Integer.parseInt(userId);
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "User ID must be a valid integer");
                return;
            }

            // Verify user exists
            UserDAO userDAO = new UserDAO();
            User user = userDAO.read(userIdInt);

            if (user == null) {
                showAlert(AlertType.ERROR, "User Not Found",
                         "User with ID " + userIdInt + " does not exist in the database.\n" +
                         "Please enter a valid User ID or create a new user first.");
                System.out.println("ERROR: User ID " + userIdInt + " not found in database");
                return;
            }

            LocalDate createdDate = AssCreated.getValue();

            InsuredAssetDAO assetDAO = new InsuredAssetDAO();
            boolean success;

            if (isEditMode) {
                if (selectedAsset == null) {
                    showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
                    return;
                }

                InsuredAsset asset = new InsuredAsset(
                    selectedAsset.getId(),
                    name,
                    type,
                    assetValue,
                    description,
                    selectedAsset.getCreatedAt(),
                    userIdInt
                );

                if (createdDate != null) {
                    asset.setCreatedAt(createdDate.atStartOfDay());
                }

                success = assetDAO.update(asset);

                if (success) {
                    showAlert(AlertType.INFORMATION, "Updated", "Insured Asset updated successfully!");
                    System.out.println("Asset updated successfully: " + asset);
                } else {
                    showAlert(AlertType.ERROR, "Update Failed", "Failed to update Insured Asset.");
                    System.out.println("Failed to update asset: " + asset);
                }
            } else {
                InsuredAsset asset = new InsuredAsset(
                    name,
                    type,
                    assetValue,
                    description,
                    userIdInt
                );

                if (createdDate != null) {
                    asset.setCreatedAt(createdDate.atStartOfDay());
                }

                success = assetDAO.create(asset);

                if (success) {
                    showAlert(AlertType.INFORMATION, "Success",
                             "Insured Asset created successfully!");
                    System.out.println("Asset created successfully: " + asset);
                } else {
                    showAlert(AlertType.ERROR, "Database Error",
                             "Failed to create Insured Asset. Please check the console for details.");
                    System.out.println("Failed to create asset: " + asset);
                }
            }

            if (success) {
                clearForm();
                refreshAssetsTable();
                setEditMode(false);
                mainTabs.getSelectionModel().select(assetListTab);
            }

        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Unexpected Error",
                     "An error occurred: " + e.getMessage());
            System.out.println("Error in handleSubmitButton: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteAssetButton() {
        if (selectedAsset == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
            return;
        }

        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        boolean deleted = assetDAO.delete(selectedAsset.getId());

        if (deleted) {
            System.out.println("Deleted asset id: " + selectedAsset.getId());
            showAlert(AlertType.INFORMATION, "Deleted", "Insured Asset deleted successfully.");
            refreshAssetsTable();
        } else {
            showAlert(AlertType.ERROR, "Delete Failed", "Failed to delete the selected asset.");
        }
    }

    /**
     * Show an alert dialog to the user
     */
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Clear all form fields after successful submission
     */
    private void clearForm() {
        AssName.clear();
        AssType.clear();
        AssValue.clear();
        AssDesc.clear();
        AssUser.clear();
        AssCreated.setValue(null);
    }
}
