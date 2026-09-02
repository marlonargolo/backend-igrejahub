package com.igrejahub.notifications.repository;

import com.igrejahub.notifications.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Existentes — mantidos
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId")
    void markAllReadByUserId(Long userId);

    // Filtrados por Igreja
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId " +
           "AND (:churchId IS NULL OR n.churchId = :churchId) " +
           "ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndChurchId(
        @Param("userId") Long userId,
        @Param("churchId") Long churchId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId " +
           "AND n.read = false " +
           "AND (:churchId IS NULL OR n.churchId = :churchId)")
    long countByUserIdAndReadFalseAndChurchId(
        @Param("userId") Long userId,
        @Param("churchId") Long churchId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId " +
           "AND (:churchId IS NULL OR n.churchId = :churchId)")
    void markAllReadByUserIdAndChurchId(
        @Param("userId") Long userId,
        @Param("churchId") Long churchId);
}