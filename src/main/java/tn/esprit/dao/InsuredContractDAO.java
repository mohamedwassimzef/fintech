package tn.esprit.dao;

import tn.esprit.entities.InsuredContract;
import tn.esprit.enums.ContractStatus;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for InsuredContract entity.
 * Implements CRUD operations for InsuredContract in the database.
 */
public class InsuredContractDAO implements CrudInterface<InsuredContract> {

    private Connection connection;

    public InsuredContractDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(InsuredContract entity) {
        String query = "INSERT INTO insured_contract (contract_number, asset_id, user_id, start_date, end_date, " +
                "premium_amount, coverage_amount, status, created_at, approved_by) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getContractNumber());
            pstmt.setInt(2, entity.getAssetId());
            pstmt.setInt(3, entity.getUserId());
            pstmt.setDate(4, entity.getStartDate() != null ? Date.valueOf(entity.getStartDate()) : null);
            pstmt.setDate(5, entity.getEndDate() != null ? Date.valueOf(entity.getEndDate()) : null);
            pstmt.setDouble(6, entity.getPremiumAmount());
            pstmt.setDouble(7, entity.getCoverageAmount());
            pstmt.setString(8, entity.getStatus() != null ? entity.getStatus().name() : ContractStatus.PENDING.name());
            pstmt.setTimestamp(9, Timestamp.valueOf(entity.getCreatedAt() != null ?
                    entity.getCreatedAt() : LocalDateTime.now()));

            if (entity.getApprovedBy() != null) {
                pstmt.setInt(10, entity.getApprovedBy());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating InsuredContract: " + e.getMessage());
            return false;
        }
    }

    @Override
    public InsuredContract read(int id) {
        String query = "SELECT * FROM insured_contract WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading InsuredContract: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<InsuredContract> readAll() {
        List<InsuredContract> contracts = new ArrayList<>();
        String query = "SELECT * FROM insured_contract";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                contracts.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all InsuredContracts: " + e.getMessage());
        }
        return contracts;
    }

    @Override
    public boolean update(InsuredContract entity) {
        String query = "UPDATE insured_contract SET contract_number = ?, asset_id = ?, user_id = ?, " +
                "start_date = ?, end_date = ?, premium_amount = ?, coverage_amount = ?, " +
                "status = ?, created_at = ?, approved_by = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getContractNumber());
            pstmt.setInt(2, entity.getAssetId());
            pstmt.setInt(3, entity.getUserId());
            pstmt.setDate(4, entity.getStartDate() != null ? Date.valueOf(entity.getStartDate()) : null);
            pstmt.setDate(5, entity.getEndDate() != null ? Date.valueOf(entity.getEndDate()) : null);
            pstmt.setDouble(6, entity.getPremiumAmount());
            pstmt.setDouble(7, entity.getCoverageAmount());
            pstmt.setString(8, entity.getStatus() != null ? entity.getStatus().name() : ContractStatus.PENDING.name());
            pstmt.setTimestamp(9, Timestamp.valueOf(entity.getCreatedAt() != null ?
                    entity.getCreatedAt() : LocalDateTime.now()));

            if (entity.getApprovedBy() != null) {
                pstmt.setInt(10, entity.getApprovedBy());
            } else {
                pstmt.setNull(10, Types.INTEGER);
            }

            pstmt.setInt(11, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating InsuredContract: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM insured_contract WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting InsuredContract: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a ResultSet row to an InsuredContract entity.
     *
     * @param rs The ResultSet to map
     * @return The mapped InsuredContract entity
     * @throws SQLException if there's an error accessing the ResultSet
     */
    private InsuredContract mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String contractNumber = rs.getString("contract_number");
        int assetId = rs.getInt("asset_id");
        int userId = rs.getInt("user_id");

        Date startDateSql = rs.getDate("start_date");
        LocalDate startDate = startDateSql != null ? startDateSql.toLocalDate() : null;

        Date endDateSql = rs.getDate("end_date");
        LocalDate endDate = endDateSql != null ? endDateSql.toLocalDate() : null;

        double premiumAmount = rs.getDouble("premium_amount");
        double coverageAmount = rs.getDouble("coverage_amount");

        String statusStr = rs.getString("status");
        ContractStatus status = statusStr != null ? ContractStatus.valueOf(statusStr.toUpperCase()) : ContractStatus.PENDING;

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTimestamp != null ? createdAtTimestamp.toLocalDateTime() : null;

        int approvedByValue = rs.getInt("approved_by");
        Integer approvedBy = rs.wasNull() ? null : approvedByValue;

        return new InsuredContract(id, contractNumber, assetId, userId, startDate, endDate,
                premiumAmount, coverageAmount, status, createdAt, approvedBy);
    }

    /**
     * Find contracts by user ID.
     *
     * @param userId The user ID to search for
     * @return List of contracts belonging to the user
     */
    public List<InsuredContract> findByUserId(int userId) {
        List<InsuredContract> contracts = new ArrayList<>();
        String query = "SELECT * FROM insured_contract WHERE user_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                contracts.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding contracts by user ID: " + e.getMessage());
        }
        return contracts;
    }

    /**
     * Find contracts by asset ID.
     *
     * @param assetId The asset ID to search for
     * @return List of contracts for the asset
     */
    public List<InsuredContract> findByAssetId(int assetId) {
        List<InsuredContract> contracts = new ArrayList<>();
        String query = "SELECT * FROM insured_contract WHERE asset_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, assetId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                contracts.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding contracts by asset ID: " + e.getMessage());
        }
        return contracts;
    }

    /**
     * Find contracts by status.
     *
     * @param status The contract status to search for
     * @return List of contracts with the specified status
     */
    public List<InsuredContract> findByStatus(ContractStatus status) {
        List<InsuredContract> contracts = new ArrayList<>();
        String query = "SELECT * FROM insured_contract WHERE status = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status.name());
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                contracts.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding contracts by status: " + e.getMessage());
        }
        return contracts;
    }
}

