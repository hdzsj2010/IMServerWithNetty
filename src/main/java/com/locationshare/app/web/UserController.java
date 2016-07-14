package com.locationshare.app.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import redis.clients.jedis.Jedis;

import com.locationshare.app.model.Message;
import com.locationshare.app.model.User;
import com.locationshare.app.service.UserService;
import com.locationshare.utils.MD5;
import com.locationshare.utils.redis.JedisPoolUtils;
import com.mysql.fabric.xmlrpc.base.Array;
/**
 * @author Jason_zh
 * @date 2016��6��1��
 * @version 1.0
 */
@Controller
@RequestMapping("/user")
public class UserController {
	@Autowired
	UserService userService;

	@ResponseBody
	@RequestMapping("/hello")
	public String hello(){
		return "hello world";
	}
	
	@ResponseBody
	@RequestMapping(value="/{id}",method=RequestMethod.GET)
	public Map<String, Object> getUser(@PathVariable Integer id,ModelMap model){
		Map<String, Object> map = new HashMap<String, Object>();
		User user = userService.getUserById(id);
		List<User> users = new ArrayList<User>();
		users.add(user);
		users.add(user);
		map.put("users", users);
		map.put("status", "1");
		return map;
	}
	
	/*@RequestMapping(value="/index",method=RequestMethod.GET)
	public String center(HttpServletRequest request,Model model){
		HttpSession session = request.getSession(); 
		User user = (User)session.getAttribute("user");
		if (user!=null) {
			model.addAttribute("user",user);
		}
		return "index";
	}*/
	
	@RequestMapping(value="/login",method=RequestMethod.GET)
	public String login(HttpSession session){
		if(session.getAttribute("user")!=null)
			return "redirect:/user/index.do";
		
		return "login";
	}
	
	@RequestMapping(value="/index",method=RequestMethod.GET)
	public String index(HttpSession session){
		return "index";
	}
	
	/*��ҳ��������Ϣ���ȴ�redis����
	 * ��Ҫע������⣺
	 * 1.���ѹ�ϵ�����ʱ����Ҫ����˫����redis key
	 * 2.������Ϣ�����ʱ����Ҫ���¸�����Ϣ��redis key
	 */
	@ResponseBody
	@RequestMapping(value="/index_info",method=RequestMethod.GET)
	public Map<String, Object> index(@RequestParam String username){
		Map<String, Object> map = new HashMap<String, Object>();
		User user = userService.getUserByName(username);
		//��Ҫ�쳣����
		if (user==null) {
			return map;
		}
		map.put("userinfo", user);
		
		//redis���غ�����Ϣ,redis�д洢�ĺ����б����sorted set����ʽ��������������˳��������У��˴���ȡ�õ��ĺ��Ѿ��ǰ���������������
		Jedis jedis = JedisPoolUtils.getJedis();
		Set<String> friNames = jedis.zrevrange(user.getUsername(), 0, -1);
	    
		//redis�в����ڸ��û��ĺ����б�������ݿ���ز�����redis
		//�˴���Ҫ������ƣ������MySQL��ȡ���ݣ��������������������
		if (friNames==null || friNames.size()==0) {
			friNames = new TreeSet<String>();
			List<User> friends = userService.findFriends(user.getId());
			for (User user3 : friends) {
				jedis.zadd(user.getUsername(), 1, user3.getUsername());
	        	//jedis.lpush(user.getUsername(), user3.getUsername());
	        	friNames.add(user3.getUsername());
			}
		}
		List<Map<String, String>> friendsList = new ArrayList<Map<String,String>>();
		//���Դ�redis��ȡ������Ϣ
		for (String username2 : friNames) {
			
			Map<String, String> tempMap = new HashMap<String, String>();
			List<String> friInfo = jedis.hmget(username2+"info", "userid","username","nickname","url");
			//redis�иú�����Ϣ�����ڣ�������ݿ���ز�����redis
			int friendid;
			if (friInfo.get(0)==null && friInfo.get(1)==null && friInfo.get(2)==null) {
				User userTemp = userService.getUserByName(username2);
				friendid = userTemp.getId();
				if (userTemp!=null) {
					tempMap.put("userid", userTemp.getId()!=null?userTemp.getId().toString():"");
					tempMap.put("username", userTemp.getUsername()!=null?userTemp.getUsername():"");
					tempMap.put("nickname", userTemp.getNickname()!=null?userTemp.getNickname():"");
					tempMap.put("url", userTemp.getUrl()!=null?userTemp.getUrl():"");
				}
				jedis.hmset(username2+"info", tempMap);
			//redis�д��ڸú�����Ϣ��ֱ�ӷ�װ
			}else{
				tempMap.put("username", friInfo.get(1));
				tempMap.put("nickname", friInfo.get(2));
				tempMap.put("url", friInfo.get(3));
				friendid = Integer.parseInt(friInfo.get(0));
			}
			
			String message = "";
			//��redis�м������µ�һ��������Ϣ
			List<String> messageFromRedis;
			if(username.hashCode() < username2.hashCode()){
				messageFromRedis = jedis.lrange(username+username2, 0, 1);
			}else {
				messageFromRedis = jedis.lrange(username2+username, 0, 1);
			}
			
			if (messageFromRedis.size()!=0) {
				message = messageFromRedis.get(0);
			}else{
				//redis�в����ڣ����ܻ����������崻������⣩�����MySQL����
				List<Message> messages = userService.getLastMessages(user.getId(),friendid);
				if(messages.get(0)!=null)
					message = messages.get(0).getContext();
				else
					//���û����Ϣ����˵���Ѿ���ѯ��������Ч�����ڵĺ��������¼
					break;
			}
			tempMap.put("last_message", message);
			friendsList.add(tempMap);
		}
        map.put("friends", friendsList);
		//model.addAttribute("status", map.get("status"));
		//model.addAttribute("user",map.get("user"));
		return map;
	}
	
	@ResponseBody
	@RequestMapping(value="/login",method=RequestMethod.POST)
	public Map<String, String> login(@ModelAttribute("user") User user,HttpSession session){
		Map<String, String> map = new HashMap<String, String>();
		user.setPassword(MD5.GetMD5Code(user.getPassword()));
		String status = userService.login(user,session);
		map.put("status", status);
		//model.addAttribute("status", map.get("status"));
		//model.addAttribute("user",map.get("user"));
		return map;
	}
	
	@RequestMapping(value="/register",method=RequestMethod.GET)
	public String register_form(HttpSession session){
		if(session.getAttribute("user")!=null){
			//return new ModelAndView("redirect:/user/center.do");
			return "redirect:/user/index.do";
		}
		return "register";
	}
	
	//�����û����Ƿ����
	 @ResponseBody
	 @RequestMapping(value="/register/checkUserName",method = RequestMethod.POST)  
	 public Map<String, Object> checkUserName(@RequestParam String username){
	    boolean isExist=userService.isNameExist(username);
	    Map<String,Object> map = new HashMap<String,Object>();    
	    map.put("isExist", isExist);              
	    return map;
	}
	
	@ResponseBody
	@RequestMapping(value="register",method=RequestMethod.POST)
	public Map<String, Object> register(@ModelAttribute("user") User user,HttpSession session){
		Map<String,Object> map = new HashMap<String,Object>();
		
		//MD5����
		String password = MD5.GetMD5Code(user.getPassword());
		user.setPassword(password);
		
		if(userService.register(user)==1){
	        //map.put("userinfo", user);
	        map.put("status", "1");  //ע��ɹ�
	        session.setAttribute("user", user);
	        return map;
			//return new ModelAndView("usercenter",map);
		}
		else {
			map.put("status", "2"); //ע��ʧ��
			return map;
			//return new ModelAndView("redirect:/user/register_form.do");
		}
	}
	
	@ResponseBody
	@RequestMapping(value="message")
	public List<String> message(@RequestParam("username")String username,@RequestParam("friendname")String friendname){
		Jedis jedis = JedisPoolUtils.getJedis();
		List<String> messagesAfter = new ArrayList<>();
		if (username.hashCode()<friendname.hashCode()) {
			List<String> messages = jedis.lrange(username+friendname, 0, 3);
			/*���redis��û����Ϣ�����Դ����ݿ�ȡ��Ϣ
			 * TODO
			 * */
			for (String mes : messages) {
				if(mes.startsWith("S:"))
					mes.replaceFirst("S:", "M:");
				else if(mes.startsWith("R:"))
					mes.replaceFirst("R:", "O:");
				messagesAfter.add(mes);
			}
		}else {
			List<String> messages = jedis.lrange(friendname+username, 0, 3);
			/*���redis��û����Ϣ�����Դ����ݿ�ȡ��Ϣ
			 * TODO
			 * */
			for (String mes : messages) {
				if(mes.startsWith("S:"))
					mes.replaceFirst("S:", "O:");
				else if(mes.startsWith("R:"))
					mes.replaceFirst("R:", "M:");
				messagesAfter.add(mes);
			}
		}
		return messagesAfter;
	}
	
	@ResponseBody
	@RequestMapping(value="message")
	public List<Map<String, String>> message(@RequestParam("username")String username){
		//redis���غ�����Ϣ,redis�д洢�ĺ����б����sorted set����ʽ��������������˳��������У��˴���ȡ�õ��ĺ��Ѿ��ǰ���������������
		Jedis jedis = JedisPoolUtils.getJedis();
		Set<String> friNames = jedis.zrevrange(username, 0, -1);
		
		List<Map<String, String>> friendsList = new ArrayList<Map<String,String>>();
		//���Դ�redis��ȡ������Ϣ
		for (String username2 : friNames) {
			Map<String, String> tempMap = new HashMap<String, String>();
			List<String> friInfo = jedis.hmget(username2+"info", "userid","username","nickname","url");
			//redis�иú�����Ϣ�����ڣ�������ݿ���ز�����redis
			if (friInfo.get(0)==null && friInfo.get(1)==null && friInfo.get(2)==null) {
				User userTemp = userService.getUserByName(username2);
				if (userTemp!=null) {
					tempMap.put("userid", userTemp.getId()!=null?userTemp.getId().toString():"");
					tempMap.put("username", userTemp.getUsername()!=null?userTemp.getUsername():"");
					tempMap.put("nickname", userTemp.getNickname()!=null?userTemp.getNickname():"");
					tempMap.put("url", userTemp.getUrl()!=null?userTemp.getUrl():"");
				}
				jedis.hmset(username2+"info", tempMap);
			//redis�д��ڸú�����Ϣ��ֱ�ӷ�װ
			}else{
				tempMap.put("username", friInfo.get(1));
				tempMap.put("nickname", friInfo.get(2));
				tempMap.put("url", friInfo.get(3));
				//��Ҫ��һ��signature��Ŀǰredis��û�У�ͬʱurlҪ�ĳ�portraitURL
			}
			friendsList.add(tempMap);
		}
		return friendsList;
	}
}
