package com.example.security.service.impl;

import com.example.security.entity.Role;
import com.example.security.entity.User;
import com.example.security.jwt.JwtTokenUtil;
import com.example.security.mapper.RoleMapper;
import com.example.security.mapper.UserMapper;
import com.example.security.service.UserService;
import com.example.security.util.*;
import com.sun.deploy.panel.TreeBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * @Author:YangWenbin
 * @Description：
 * @Date:21:38 2019/1/5
 * @ModifiedBy:
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RoleMapper roleMapper;



    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 获取用户信息
     * @param username
     * @return
     */
    @Override
    public User findByUsername(String username) {
        User user = userMapper.selectByUserName(username);
        log.info("userserviceimpl"+user);
        return user;
    }

    /**
     * 登录方法
     * @param username
     * @param password
     * @return
     * @throws AuthenticationException
     */
    @Override
    public RetResult login(String username, String password) throws AuthenticationException {
        UsernamePasswordAuthenticationToken upToken = new UsernamePasswordAuthenticationToken(username, password);
        final Authentication authentication = authenticationManager.authenticate(upToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new RetResult(RetCode.SUCCESS.getCode(),"登录成功",jwtTokenUtil.generateToken(userDetails));
    }

    /**
     * 获取用户信息，包括角色列表，权限资源，返回给前端使用
     * @param username
     * @return
     */
    @Override
    public RetResult getUserInfo(String username) {
        User user = userMapper.selectByUserName(username);
        //查询出改用户下的角色和权限
        Set<Role> roles = roleMapper.selectByUserName(username);
        Set<Menu> menus = new HashSet<>();
        roles.forEach( role -> {
            role.getPermissions().forEach( permission -> {
                //这里应该type为menu的，button目前还未添加
                if(permission.getPer_type().equals("menu")){
                    menus.add(new Menu(permission.getPer_id(),permission.getPer_parent_id(),permission.getPer_name(),
                            permission.getPer_resource()));
                }
            });
        });
        UserVo userVo =new UserVo(user.getId(),user.getUsername(),user.getRoles(),GenTree.genRoot(menus));
        return new RetResult(RetCode.SUCCESS.getCode(),userVo);

    }

    /**
     * 获取菜单树
     * @param username
     * @return
     */
    @Override
    public RetResult getMenuTree(String username) {
        //目前只做菜单权限以后增加按钮的权限控制
        Set<Role> permissions = roleMapper.selectByUserName(username);
        Set<Menu> menus =new HashSet<>();
        //遍历上面得到的权限列表
        permissions.forEach(role -> {
            //遍历权限
            role.getPermissions().forEach( permission -> {
                if (permission.getPer_type().equals("menu")){
                    log.info(permission.getPer_name());
                    menus.add(new Menu(permission.getPer_id(),permission.getPer_parent_id(),permission.getPer_name(),
                            permission.getPer_resource()));
                    for (Menu menu:menus){
                        System.out.println(menu);
                    }
                }
            });
        });
        return new RetResult(RetCode.SUCCESS.getCode(),"获取菜单树成功",GenTree.genRoot(menus));
    }


}
