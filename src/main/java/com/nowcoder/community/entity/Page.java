package com.nowcoder.community.entity;
/*
封装分页相关的信息
 */
public class Page {
	// 当前页码
	private int current = 1;
	//显示的上限
	private int limit = 10;
	//数据的总数(计算总页数)
	private int rows;
	//查询路径（用于复用分页链接)
	private String path;

	public int getCurrent() {
		return current;
	}

	public void setCurrent(int current) {
		//判断输入的页码是否合法
		if(current >= 1) {
			this.current = current;
		}
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		//限制每页上限数
		if(limit >= 1 && limit <= 100){
		this.limit = limit;
		}
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		if(rows >= 0){
		this.rows = rows;
		}
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	//获取当前页的起始行
	public int getOffset(){
		//current *limit - limit
		return (current - 1) * limit ;
	}
	//获取总页数
	public int getTotal(){
		if(rows % limit ==0){
			return rows / limit;
		}else{
			return  rows / limit + 1;
		}
	}
	//获取起始页码
	public int getFrom(){
		int from = current - 2;
		return from < 1 ? 1 : from;
	}
	//获取结束页码
	public int getTo(){
		int to = current + 2;
		int total = getTotal();
		return to > total ? total : to;
	}
}
