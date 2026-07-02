package com.beewax.service;

import com.beewax.dto.request.LoginRequest;
import com.beewax.dto.response.LoginResponse;
import com.beewax.dto.response.UserInfoResponse;
import com.beewax.entity.User;
import com.beewax.repository.UserRepository;
import com.beewax.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
	}

	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.getUsername())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码错误"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号或密码错误");
		}

		if (user.getStatus() == User.UserStatus.INACTIVE) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已停用");
		}

		String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
		UserInfoResponse userInfo = new UserInfoResponse(user.getId(), user.getName(), user.getRole().name());

		return new LoginResponse(token, userInfo);
	}
}
