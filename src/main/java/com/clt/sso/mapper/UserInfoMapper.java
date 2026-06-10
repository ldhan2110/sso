package com.clt.sso.mapper;

import org.apache.ibatis.annotations.Param;

import com.clt.sso.model.UserInfoModel;

public interface UserInfoMapper {
    UserInfoModel findByTentIdAndUsrId(@Param("tentId") String tentId,
                                        @Param("usrId") String usrId);
    UserInfoModel findByTentIdAndUsrNm(@Param("tentId") String tentId,
                                        @Param("usrNm") String usrNm);
}
