package com.clt.sso.utils;

import java.io.Reader;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

public class SessionFactory {
	private static SqlSessionFactory sqlSessionFactory;

	static {
		try {
			// Load MyBatis configuration file
			String resource = "mybatis-config.xml";
			Reader reader = Resources.getResourceAsReader(resource);

			// Build SqlSessionFactory
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
		} catch (Exception e) {
			e.printStackTrace(); // Log properly in real code
		}
	}

	public static SqlSession getSqlSession() {
		return sqlSessionFactory.openSession(); // Opens a new SqlSession
	}
}