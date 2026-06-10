package com.clt.sso.utils;

import java.io.Reader;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.jboss.logging.Logger;

public class SessionFactory {
	private static final Logger LOG = Logger.getLogger(SessionFactory.class);
	private static final SqlSessionFactory sqlSessionFactory;

	static {
		try {
			String resource = "mybatis-config.xml";
			Reader reader = Resources.getResourceAsReader(resource);
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
			LOG.info("MyBatis SqlSessionFactory initialized successfully");
		} catch (Exception e) {
			LOG.error("Failed to initialize MyBatis SqlSessionFactory", e);
			throw new ExceptionInInitializerError(e);
		}
	}

	public static SqlSession getSqlSession() {
		return sqlSessionFactory.openSession();
	}
}