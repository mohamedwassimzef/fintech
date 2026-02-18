package tn.esprit.app;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.entities.InsuredContract;
import tn.esprit.enums.ContractStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class main {

    public static void main(String[] args){
        System.out.println("hello");
        InsuredAssetDAO insuredAssetDAO = new InsuredAssetDAO();
        InsuredContractDAO insuredContractDAO = new InsuredContractDAO();
        InsuredContract insuredContract = new InsuredContract(
                1,                                          // id
                "IC-2026-" + System.currentTimeMillis(),    // contractNumber (unique)
                29,                                         // assetId (Tesla Model 3 Updated)
                127,                                        // userId (matching the asset owner)
                LocalDate.of(2026, 2, 8),                  // startDate
                LocalDate.of(2027, 2, 8),                  // endDate
                1000.0,                                     // premiumAmount
                50000.0,                                    // coverageAmount
                ContractStatus.ACTIVE,                      // status
                LocalDateTime.now(),                        // createdAt
                null                                        // approvedBy
        );
        System.out.println("All Insured Assets:");
        System.out.println(insuredAssetDAO.readAll());

        Boolean created = insuredContractDAO.create(insuredContract);
        System.out.println("\nInsured Contract created: " + created);

        System.out.println("\nAll Insured Contracts:");
        System.out.println(insuredContractDAO.readAll());


    }
}
