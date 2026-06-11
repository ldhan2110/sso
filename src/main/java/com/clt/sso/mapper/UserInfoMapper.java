package com.clt.sso.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.clt.sso.model.UserInfoModel;

public interface UserInfoMapper {
    UserInfoModel findByTentIdAndUsrId(@Param("tentId") String tentId,
                                        @Param("usrId") String usrId);
    UserInfoModel findByTentIdAndUsrNm(@Param("tentId") String tentId,
                                        @Param("usrNm") String usrNm);
    List<UserInfoModel> searchUsers(@Param("search") String search,
                                     @Param("firstResult") int firstResult,
                                     @Param("maxResults") int maxResults);
    int countUsers(@Param("search") String search);
}
