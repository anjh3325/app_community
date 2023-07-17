package com.company.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.company.exception.NotExistPostException;
import com.company.exception.NotExistUserException;
import com.company.model.dto.PostWrapper;
import com.company.model.dto.ReReplyWrapper;
import com.company.model.dto.ReplyWrapper;
import com.company.model.dto.post.request.CreatePostRequest;
import com.company.model.dto.post.request.UpdatePostRequest;
import com.company.model.dto.post.response.AllPostsResponse;
import com.company.model.entity.Image;
import com.company.model.entity.Post;
import com.company.model.entity.Recommend;
import com.company.model.entity.Reply;
import com.company.model.entity.User;
import com.company.repository.ImageRepository;
import com.company.repository.PostRepository;
import com.company.repository.RecommendRepository;
import com.company.repository.ReplyRepository;
import com.company.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

	@Value("${upload.server}")
	String uploadServer;
	@Value("${upload.basedir}")
	String uploadBaseDir;

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ImageRepository imageRepository;
	private final RecommendRepository recommendRepository;

	private final ReplyRepository replyRepository;

	public AllPostsResponse allPosts() {
		List<Post> postLi = postRepository.findAll();

		List<PostWrapper> li = postLi.stream().map(e -> new PostWrapper(e)).toList();

		Long cnt = postRepository.count();

		return new AllPostsResponse(cnt, li);

	}

	public void save(String principal, CreatePostRequest req)
			throws NotExistUserException, IllegalStateException, IOException {
		User user = userRepository.findByEmail(principal).orElseThrow(() -> new NotExistUserException());
		Post post = new Post(req, user);
		Post saved = postRepository.save(post);

		if (req.getAttaches() != null) {
			File uploadDirectory = new File(uploadBaseDir + "/post/" + saved.getId());
			uploadDirectory.mkdirs();

			for (MultipartFile multi : req.getAttaches()) {
				String fileName = String.valueOf(System.currentTimeMillis());
				String extension = multi.getOriginalFilename().split("\\.")[1];
				File dest = new File(uploadDirectory, fileName + "." + extension);

				multi.transferTo(dest);

				Image image = new Image();

				image.setImageUrl(uploadServer + "/post/" + saved.getId() + "/" + fileName + "." + extension);
				image.setPostsId(saved);

				imageRepository.save(image);

			}
		}
	}

	public void update(String principal, UpdatePostRequest req) throws NotExistUserException, NotExistPostException {
		userRepository.findByEmail(principal).orElseThrow(() -> new NotExistUserException());
		Integer id = req.getId();

		Post post = postRepository.findById(id).orElseThrow(() -> new NotExistPostException());

		Post saved = new Post(post, req);

		postRepository.save(saved);

	}

	public PostWrapper getSpecificPost(Integer id) throws NumberFormatException, NotExistPostException {
		Post data = postRepository.findById(id).orElseThrow(() -> new NotExistPostException());

		Post post = new Post(data);

		postRepository.save(post);

		if (replyRepository.findByPostsId(post).size() == 0) {
			int recommendCnt = recommendRepository.findByPostsId(post).size();

			return new PostWrapper(post, recommendCnt);
		} else {

			List<Reply> replyDatas = replyRepository.findByPostsId(post);
			List<Reply> replyLi = replyDatas.stream().filter(t -> t.getParentId() == null).toList();

			List<ReReplyWrapper> reReplyWrapperLi = new ArrayList<>();
			List<ReplyWrapper> replyWrapperLi = new ArrayList<>();

			for (Reply out : replyLi) {

				for (Reply in : replyDatas) {
					if (out.getId().equals(in.getParentId())) {
						int recommendCnt = recommendRepository.findByRepliesId(in).size();

						reReplyWrapperLi.add(new ReReplyWrapper(in, recommendCnt));
					}
				}
				int recommendCnt = recommendRepository.findByRepliesId(out).size();
				ReplyWrapper replyWrapper = new ReplyWrapper(out, reReplyWrapperLi, recommendCnt);

				replyWrapperLi.add(replyWrapper);
			}

			int recommendCnt = recommendRepository.findByPostsId(post).size();

			return new PostWrapper(post, replyWrapperLi, recommendCnt);
		}
	}
}
