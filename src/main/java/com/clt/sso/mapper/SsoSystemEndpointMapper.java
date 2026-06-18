package com.clt.sso.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.clt.sso.model.SsoSystemEndpointModel;

public interface SsoSystemEndpointMapper {
    List<SsoSystemEndpointModel> findActiveByType(@Param("endpointType") String endpointType);
    List<SsoSystemEndpointModel> findAll();
}
