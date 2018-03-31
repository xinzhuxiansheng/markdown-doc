package com.get.markdown.doc.sqlitedao;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 获取数据库连接
 * @author yzhou
 *
 */
@Component
public class ConnectionFactory {
	
	private static String dbpath;
	
	@Value("${sqlitedb}")
	public  void setDbpath(String dbpath) {
		ConnectionFactory.dbpath = dbpath;
	}

	private static Connection conn;
	
	public static Connection getConnection(){
		try {
			if(null == conn || conn.isClosed() || !conn.isValid(1)){
		
				
				//URL dbUrl = ConnectionFactory.class.getResource(DB);
				//URL dbUrl = Thread.currentThread().getContextClassLoader().getResource("/").toURI().toURL();
				//System.out.println(dbUrl);
				String filePath = dbpath;
				String connstr = "jdbc:sqlite:"+filePath;
				System.out.println("connstr:"+ connstr);
				try {
					Class.forName("org.sqlite.JDBC");
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				conn = DriverManager.getConnection(connstr);
				
			}
		} catch (Exception e) {
			conn=null;
		}
		return conn;
	}
}
