package com.asg.shipping.importmanifestbl.repository;

import java.util.List;

public interface ContainerDropdownRepository {
    List<Object[]> findAllCommodities();
    List<Object[]> findContainerTypes(Long voyageTranPoid);
}
