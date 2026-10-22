package com.challange.inventory_service.mapper;

import com.challange.inventory_service.dto.ProductDTO;
import com.challange.inventory_service.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    ProductEntity mapToProductEntity(ProductDTO productDTO);

    ProductDTO mapToProductDTO(ProductEntity productEntity);
}
