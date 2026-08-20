package com.docplatform.documentservice.repository

import com.docplatform.documentservice.entity.Document
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DocumentRepository : JpaRepository<Document, UUID> {

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.shares WHERE d.id = :id")
    fun findByIdWithShares(id: UUID): Optional<Document>

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.versions WHERE d.id = :id")
    fun findByIdWithVersions(id: UUID): Optional<Document>

    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.versions LEFT JOIN FETCH d.shares WHERE d.id = :id")
    fun findByIdWithSharesAndVersions(id: UUID): Optional<Document>

    fun findByOwnerId(ownerId: UUID, pageable: Pageable): Page<Document>

    fun findByFileId(fileId: UUID): Document?

    @Query(
        value = """
            SELECT d FROM Document d
            WHERE d.ownerId = :userId
                OR d.companyWide = true
                OR EXISTS (
                    SELECT 1 FROM DocumentShare s
                    WHERE s.document = d
                        AND (
                            (s.shareType = 'USER' AND s.targetId = :userId)
                            OR s.shareType = 'COMPANY'
                        )
                )
        """,
        countQuery = """
            SELECT COUNT(d) FROM Document d
            WHERE d.ownerId = :userId
                OR d.companyWide = true
                OR EXISTS (
                    SELECT 1 FROM DocumentShare s
                    WHERE s.document = d
                        AND (
                            (s.shareType = 'USER' AND s.targetId = :userId)
                            OR s.shareType = 'COMPANY'
                        )
                )
        """
    )
    fun findAccessibleByUser(userId: UUID, pageable: Pageable): Page<Document>

    @Query(
        value = """
            SELECT d FROM Document d
            WHERE d.ownerId = :userId
                OR d.companyWide = true
                OR EXISTS (
                    SELECT 1 FROM DocumentShare s
                    WHERE s.document = d
                        AND (
                            (s.shareType = 'USER' AND s.targetId = :userId)
                            OR (s.shareType = 'DEPARTMENT' AND s.targetId IN :departmentIds)
                            OR s.shareType = 'COMPANY'
                        )
                )
        """,
        countQuery = """
            SELECT COUNT(d) FROM Document d
            WHERE d.ownerId = :userId
                OR d.companyWide = true
                OR EXISTS (
                    SELECT 1 FROM DocumentShare s
                    WHERE s.document = d
                        AND (
                            (s.shareType = 'USER' AND s.targetId = :userId)
                            OR (s.shareType = 'DEPARTMENT' AND s.targetId IN :departmentIds)
                            OR s.shareType = 'COMPANY'
                        )
                )
        """
    )
    fun findAccessibleByUserAndDepartments(
        userId: UUID,
        departmentIds: List<UUID>,
        pageable: Pageable
    ): Page<Document>

    @Query("SELECT DISTINCT d FROM Document d JOIN d.shares s WHERE s.shareType = 'DEPARTMENT' AND s.targetId = :departmentId")
    fun findByDepartmentShare(departmentId: UUID, pageable: Pageable): Page<Document>

    @Query(
        value = """
            SELECT d.* FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
            GROUP BY d.id
        """,
        countQuery = """
            SELECT COUNT(DISTINCT d.id) FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
        """,
        nativeQuery = true
    )
    fun findAllWithSearchAndFilters(
        search: String?,
        category: String?,
        tag: String?,
        pageable: Pageable
    ): Page<Document>

    @Query(
        value = """
            SELECT d.* FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (d.owner_id = CAST(:userId AS UUID)
                OR d.company_wide = true
                OR EXISTS (
                    SELECT 1 FROM document_shares s
                    WHERE s.document_id = d.id
                        AND ((s.share_type = 'USER' AND s.target_id = CAST(:userId AS UUID))
                            OR s.share_type = 'COMPANY')
                ))
                AND (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
            GROUP BY d.id
        """,
        countQuery = """
            SELECT COUNT(DISTINCT d.id) FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (d.owner_id = CAST(:userId AS UUID)
                OR d.company_wide = true
                OR EXISTS (
                    SELECT 1 FROM document_shares s
                    WHERE s.document_id = d.id
                        AND ((s.share_type = 'USER' AND s.target_id = CAST(:userId AS UUID))
                            OR s.share_type = 'COMPANY')))
                AND (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
        """,
        nativeQuery = true
    )
    fun findAccessibleByUserWithSearchAndFilters(
        userId: UUID,
        search: String?,
        category: String?,
        tag: String?,
        pageable: Pageable
    ): Page<Document>

    @Query(
        value = """
            SELECT d.* FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (d.owner_id = CAST(:userId AS UUID)
                OR d.company_wide = true
                OR EXISTS (
                    SELECT 1 FROM document_shares s
                    WHERE s.document_id = d.id
                        AND ((s.share_type = 'USER' AND s.target_id = CAST(:userId AS UUID))
                            OR (s.share_type = 'DEPARTMENT' AND s.target_id IN (:departmentIds))
                            OR s.share_type = 'COMPANY')
                ))
                AND (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
            GROUP BY d.id
        """,
        countQuery = """
            SELECT COUNT(DISTINCT d.id) FROM documents d
            LEFT JOIN document_tags t ON d.id = t.document_id
            WHERE (d.owner_id = CAST(:userId AS UUID)
                OR d.company_wide = true
                OR EXISTS (
                    SELECT 1 FROM document_shares s
                    WHERE s.document_id = d.id
                        AND ((s.share_type = 'USER' AND s.target_id = CAST(:userId AS UUID))
                            OR (s.share_type = 'DEPARTMENT' AND s.target_id IN (:departmentIds))
                            OR s.share_type = 'COMPANY')))
                AND (CAST(:category AS TEXT) IS NULL OR LOWER(COALESCE(d.category, '')) = LOWER(CAST(:category AS TEXT)))
                AND (CAST(:tag AS TEXT) IS NULL OR LOWER(COALESCE(t.tag, '')) = LOWER(CAST(:tag AS TEXT)))
                AND (CAST(:search AS TEXT) IS NULL OR 
                    LOWER(d.name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.description, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.category, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(COALESCE(d.summary, '')) LIKE LOWER('%' || CAST(:search AS TEXT) || '%') OR
                    LOWER(d.file_name) LIKE LOWER('%' || CAST(:search AS TEXT) || '%'))
        """,
        nativeQuery = true
    )
    fun findAccessibleByUserAndDepartmentsWithSearchAndFilters(
        userId: UUID,
        departmentIds: List<UUID>,
        search: String?,
        category: String?,
        tag: String?,
        pageable: Pageable
    ): Page<Document>

    @Query("SELECT d FROM Document d WHERE (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(d.fileName) LIKE LOWER(CONCAT('%', :search, '%'))) AND d.ownerId = :userId")
    fun searchByOwner(search: String, userId: UUID, pageable: Pageable): Page<Document>

    fun existsByFileId(fileId: UUID): Boolean
}