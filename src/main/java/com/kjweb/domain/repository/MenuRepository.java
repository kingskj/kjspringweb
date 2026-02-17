package com.kjweb.domain.repository;

import com.kjweb.domain.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
    List<Menu> findByParentIdAndIsActiveTrueOrderBySortOrderAsc(Long parentId);
}
