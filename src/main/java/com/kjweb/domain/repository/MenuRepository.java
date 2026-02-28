package com.kjweb.domain.repository;

import com.kjweb.domain.entity.Menu;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
    List<Menu> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);
    List<Menu> findAllByUrlOrderByIdAsc(String url);
    List<Menu> findAllByOrderByIdAsc();

    @Modifying
    @Query(value = "DELETE FROM role_menus WHERE menu_id IN (:menuIds)", nativeQuery = true)
    void deleteRoleMenusByMenuIds(@Param("menuIds") List<Long> menuIds);
}
