package org.example;
import tn.esprit.utils.MyDB;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.InsuredContract;
import tn.esprit.enums.ContractStatus;
import tn.esprit.dao.InsuredAssetDAO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.


        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Driver found and loaded!");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver NOT found in classpath.");
        }

        MyDB db = MyDB.getInstance();

        // Create InsuredAsset
        InsuredAsset asset = new InsuredAsset(
                "Tesla Model 3",
                "Vehicle",
                45000.0,
                "Electric car for personal use",
                105
        );
        System.out.println("\n=== Created InsuredAsset ===");
        System.out.println(asset);
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        boolean created = assetDAO.create(asset);
        System.out.println("Asset creation status: " + created);


        InsuredAsset retrievedAsset = assetDAO.read(4);

        System.out.println("retrived ::::"+ retrievedAsset);// Assuming ID 1 exists

        assetDAO.delete(4);

        InsuredAsset retrievedAsset2 = assetDAO.read(4);

        System.out.println("retrived ::::"+ retrievedAsset2);// Assuming ID 1 exists

        asset.setId(4);
        asset.setName("clio");
        assetDAO.update(asset);

        System.out.println(asset);
    }
}