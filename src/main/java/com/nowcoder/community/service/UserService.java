package com.nowcoder.community.service;

import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {
	@Autowired
	private UserMapper userMapper;

	@Autowired
	private MailClient mailClient;

	@Autowired
	private TemplateEngine templateEngine;

//	@Autowired
//	private LoginTicketMapper loginTicketMapper;
	@Autowired
	private RedisTemplate redisTemplate;

	@Value("${community.path.domin}")
	private String domin;

	@Value("${server.servlet.context-path}")
	private String contextPath;

	public Map<String,Object> register(User user) throws IllegalAccessException {
		Map<String, Object> map = new HashMap<>();

		//空值处理
		if(user == null){
			throw new IllegalAccessException("参数不能为空！");
		}
		if(StringUtils.isBlank(user.getUsername())){
			map.put("usernameMsg","账号不能为空");
			return  map;
		}
		if(StringUtils.isBlank(user.getPassword())){
			map.put("passwordMsg","密码不能为空");
			return  map;
		}
		if(StringUtils.isBlank(user.getEmail())){
			map.put("emailMsg","邮箱不能为空");
			return  map;
		}

		//验证账号
		User u = userMapper.selectByName(user.getUsername());
		if(u != null){
			map.put("usernameMsg" , "该账号已存在");
			return  map;
		}
		u = userMapper.selectByEmail(user.getEmail());
		if(u != null){
			map.put("emailMsg" , "该邮箱已被注册");
			return map;
		}

		//注册用户
		user.setSalt(CommunityUtil.generateUUID().substring(0,5));
		user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
		user.setType(0);
		user.setStatus(0);
		user.setActivationCode(CommunityUtil.generateUUID());
		user.setHeaderUrl(String.format("http://http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
		user.setCreateTime(new Date());
		userMapper.insertUser(user);

		//发送激活邮件
		Context context = new Context();
		context.setVariable("email", user.getEmail());
		//http://localhost:8080/community/activation/101/code
		String url = domin + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
		context.setVariable("url" , url);
		String content = templateEngine.process("/mail/activation",context);
		mailClient.sendMail(user.getEmail(),"激活账号", content);
		return  map;
	}
	//激活
	public int activation(int userId, String code){
		User user = userMapper.selectById(userId);
		if(user.getStatus() == 1){
			return ACTIVATION_REPEAT;
		}
		if(user.getActivationCode().equals(code)){
			userMapper.updateStatus(userId,1);
			clearCache(userId);
			return ACTIVATION_SUCCESS;
		}else{
			return ACTIVATION_FAILURE;
		}
	}

	public Map<String, Object> login(String username, String password, int expiredSeconds){
		Map<String , Object> map = new HashMap<>();
			//空值处理
		if(StringUtils.isBlank(username)){
			map.put("usernameMsg", "账号不能为空");
			return map;
		}
		if(StringUtils.isBlank(password)){
			map.put("passwordMsg", "密码不能为空");
			return map;
		}

		//验证账号合法性
		//验证账号
		User u = userMapper.selectByName(username);
		if(u == null){
			map.put("usernameMsg" , "账号不存在");
			return map;
		}
		//验证状态
		if( u.getStatus() == 0){
			map.put("statusMsg", "该账号未激活");
			return map;
		}

		//验证密码
		password = CommunityUtil.md5(password + u.getSalt());
		if(!u.getPassword().equals(password)){
			map.put("passwordMsg" , "密码不正确");
			return map;
		}

		//通过所有条件，生成登陆凭证
		LoginTicket loginTicket = new LoginTicket();
		loginTicket.setUserId(u.getId());
		loginTicket.setTicket(CommunityUtil.generateUUID());
		loginTicket.setStatus(0);
		loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//		loginTicketMapper.insertLoginTicket(loginTicket);
		String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
		//redis会自动吧loginTicket对象转化为JSON字符串保存
		redisTemplate.opsForValue().set(redisKey , loginTicket);
		map.put("ticket" , loginTicket.getTicket());

		return map;
	}

	public LoginTicket findLoginTicket(String ticket){
//		return loginTicketMapper.selectByTicket(ticket);
		String redisKey = RedisKeyUtil.getTicketKey(ticket);
		return (LoginTicket) redisTemplate.opsForValue().get(ticket);

	}

	public void logout(String ticket){
//		loginTicketMapper.updateStatus(ticket, 1);
		String redisKey = RedisKeyUtil.getTicketKey(ticket);
		LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticket);
		loginTicket.setStatus(1);
		redisTemplate.opsForValue().set(redisKey , loginTicket);
	}

	public User findUserById(int id){
//		return userMapper.selectById(id);
		User user = getCache(id);
		if (user == null){
			user = initCache(id);
		}
		return user;
	}

	public int updateHeader(int userId, String headerUrl){
		int rows = userMapper.updateHeader(userId,headerUrl);
		clearCache(userId);
		return rows;
	}

	public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword){
		Map<String, Object> map = new HashMap<>();
		//空值处理
		if(StringUtils.isBlank(oldPassword)){
			map.put("oldPasswordMsg", "原密码不能为空");
			return map;
		}
		if(StringUtils.isBlank(newPassword)){
			map.put("newPasswordMsg", "新密码不能为空");
			return map;
		}
		//验证原始密码
		User user = userMapper.selectById(userId);
		if(!user.getPassword().equals(oldPassword)){
			map.put("oldPasswordMsg" , "原密码错误");
			return map;
		}
		//更新密码
		newPassword = CommunityUtil.md5(newPassword + user.getSalt());
		userMapper.updatePassword(userId , newPassword);
		return map;
	}

	public User findUserByName(String name){
		return userMapper.selectByName(name);
	}

	//优先从缓存中取值
	private User getCache(int userId){
		String redisKey = RedisKeyUtil.getUserKey(userId);
		return (User) redisTemplate.opsForValue().get(redisKey);
	}

	//取不到时 初始化缓存
	private User initCache(int userId){
		User user = userMapper.selectById(userId);
		String redisKey = RedisKeyUtil.getUserKey(userId);
		redisTemplate.opsForValue().set(redisKey, user, 3600 , TimeUnit.SECONDS);
		return user;
	}
	//当数据变更时，清除缓存数据
	private void clearCache(int userId){
		String redisKey = RedisKeyUtil.getUserKey(userId);
		redisTemplate.delete(redisKey);
	}
}
