package com.kjweb.web.service;

import com.kjweb.domain.entity.Menu;
import com.kjweb.domain.repository.MenuRepository;
import com.kjweb.web.dto.MenuBatchItemDto;
import com.kjweb.web.error.CustomAppException;
import com.kjweb.web.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public List<Menu> getTopMenus() {
        return menuRepository.findByParentIsNullAndIsActiveTrueOrderBySortOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<Menu> getAllMenus() {
        List<Menu> menus = menuRepository.findAllByOrderByIdAsc();
        menus.sort(Comparator
                .comparing((Menu m) -> m.getParent() == null ? 0 : 1)
                .thenComparing(m -> m.getParent() == null ? 0L : m.getParent().getId())
                .thenComparing(m -> m.getSortOrder() == null ? Integer.MAX_VALUE : m.getSortOrder())
                .thenComparing(Menu::getId));
        return menus;
    }

    @Transactional(readOnly = true)
    public Long getAdminMenuId() {
        return menuRepository.findAllByUrlOrderByIdAsc("/admin").stream()
                .findFirst()
                .map(Menu::getId)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Menu> getAdminSideMenus() {
        Long adminMenuId = getAdminMenuId();
        if (adminMenuId == null) {
            return List.of();
        }
        return menuRepository.findByParentIdAndIsActiveTrueOrderBySortOrderAsc(adminMenuId);
    }

    public Menu save(Menu menu) {
        return menuRepository.save(menu);
    }

    public void toggleActive(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.MENU_NOT_FOUND));
        menu.setIsActive(!menu.getIsActive());
    }

    public void setActive(Long id, Boolean isActive) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new CustomAppException(ErrorCode.MENU_NOT_FOUND));
        menu.setIsActive(Boolean.TRUE.equals(isActive));
    }

    public void delete(Long id) {
        menuRepository.deleteById(id);
    }

    public void saveMenusBatch(List<MenuBatchItemDto> menuItems) {
        if (menuItems == null) {
            throw new CustomAppException(ErrorCode.BAD_REQUEST, "메뉴 목록이 없습니다");
        }

        List<MenuBatchItemDto> upsertItems = menuItems.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .toList();

        Set<Long> deleteIds = new HashSet<>();
        for (MenuBatchItemDto item : menuItems) {
            if (Boolean.TRUE.equals(item.getDeleted()) && item.getId() != null) {
                deleteIds.add(item.getId());
            }
        }

        validateDeleteTargets(deleteIds);

        if (!deleteIds.isEmpty()) {
            List<Long> deleteIdList = new ArrayList<>(deleteIds);
            menuRepository.deleteRoleMenusByMenuIds(deleteIdList);

            List<Menu> deleteTargets = menuRepository.findAllById(deleteIdList);
            for (Menu deleteTarget : deleteTargets) {
                if (deleteTarget.getParent() != null && deleteIds.contains(deleteTarget.getParent().getId())) {
                    deleteTarget.setParent(null);
                }
            }
            menuRepository.deleteAll(deleteTargets);
        }

        Map<Long, Menu> allMenusById = new HashMap<>();
        for (Menu menu : menuRepository.findAllByOrderByIdAsc()) {
            allMenusById.put(menu.getId(), menu);
        }

        Set<Long> upsertIds = new HashSet<>();
        for (MenuBatchItemDto item : upsertItems) {
            if (item.getId() != null) {
                upsertIds.add(item.getId());
            }
        }

        Map<Long, Menu> existingUpsertMenus = new HashMap<>();
        if (!upsertIds.isEmpty()) {
            for (Menu menu : menuRepository.findAllById(upsertIds)) {
                existingUpsertMenus.put(menu.getId(), menu);
            }
        }

        List<Menu> saveTargets = new ArrayList<>();
        for (MenuBatchItemDto item : upsertItems) {
            Menu menu;
            if (item.getId() == null) {
                menu = new Menu();
            } else {
                menu = existingUpsertMenus.get(item.getId());
                if (menu == null) {
                    throw new CustomAppException(ErrorCode.MENU_NOT_FOUND, "메뉴를 찾을 수 없습니다: " + item.getId());
                }
            }

            Long parentId = item.getParentId();
            if (parentId == null) {
                menu.setParent(null);
            } else {
                if (item.getId() != null && parentId.equals(item.getId())) {
                    throw new CustomAppException(ErrorCode.BAD_REQUEST, "상위 메뉴는 자기 자신일 수 없습니다");
                }
                if (deleteIds.contains(parentId)) {
                    throw new CustomAppException(ErrorCode.BAD_REQUEST, "삭제 대상 메뉴를 상위 메뉴로 지정할 수 없습니다");
                }
                Menu parent = allMenusById.get(parentId);
                if (parent == null) {
                    throw new CustomAppException(ErrorCode.MENU_NOT_FOUND, "상위 메뉴를 찾을 수 없습니다: " + parentId);
                }
                menu.setParent(parent);
            }

            menu.setName(normalizeBlankToNull(item.getName()));
            menu.setUrl(normalizeBlankToNull(item.getUrl()));
            menu.setSortOrder(item.getSortOrder());
            menu.setIsActive(item.getIsActive());
            saveTargets.add(menu);
        }

        if (!saveTargets.isEmpty()) {
            menuRepository.saveAll(saveTargets);
        }
    }

    private void validateDeleteTargets(Set<Long> deleteIds) {
        if (deleteIds.isEmpty()) {
            return;
        }

        List<Menu> allMenus = menuRepository.findAllByOrderByIdAsc();
        Set<Long> allMenuIds = new HashSet<>();
        for (Menu menu : allMenus) {
            allMenuIds.add(menu.getId());
        }

        for (Long deleteId : deleteIds) {
            if (!allMenuIds.contains(deleteId)) {
                throw new CustomAppException(ErrorCode.MENU_NOT_FOUND, "삭제할 메뉴를 찾을 수 없습니다: " + deleteId);
            }
        }

        for (Menu menu : allMenus) {
            Long parentId = menu.getParent() == null ? null : menu.getParent().getId();
            if (parentId != null && deleteIds.contains(parentId) && !deleteIds.contains(menu.getId())) {
                throw new CustomAppException(ErrorCode.BAD_REQUEST, "하위 메뉴가 남아 있어 삭제할 수 없습니다: " + menu.getName());
            }
        }
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
