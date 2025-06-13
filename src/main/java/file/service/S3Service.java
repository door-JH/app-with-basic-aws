package file.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



import file.entity.AttachmentFile;
import file.repository.AttachmentFileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class S3Service {
	
	private final AmazonS3 amazonS3;
	private final AttachmentFileRepository fileRepository;
	
    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;
    
    private final String DIR_NAME = "s3_data";
    
    // 파일 업로드
	@Transactional
	public void uploadS3File(MultipartFile file) throws Exception {
		
		// D:/downloads/s3_data에 파일 저장 -> S3 전송 및 저장 (putObject)
		if(file == null) {
			throw new Exception("파일 전달 오류 발생");
		}

		//DB 저장
		String savePath = "D:\\downloads\\" + DIR_NAME;
		String attachmentOriginalFileName = file.getOriginalFilename();
		UUID uuid = UUID.randomUUID();
		String attachmentFileName = uuid.toString() + "_" + attachmentOriginalFileName;
		Long attachmentFileSize = file.getSize();

		//Entity 변환
		AttachmentFile attachmentFile = AttachmentFile.builder()
				.attachmentOriginalFileName(attachmentOriginalFileName)
				.attachmentFileName(attachmentFileName)
				.attachmentFileSize(attachmentFileSize)
				.filePath(savePath)
				.build();

		Long fileNo = fileRepository.save(attachmentFile).getAttachmentFileNo();

		//S3 Bucket에 저장
		if(fileNo != null) {
			//임시 서버(로컬)에 파일 저장
			File uploadFile = new File(attachmentFile.getFilePath() + "\\" + attachmentFileName);
			file.transferTo(uploadFile);

			//S3로 파일 전송
			// bucket : 버킷이름
			// key : 객체 저장경로 + 객체 이름
			// file : 물리 리소스
			String key = DIR_NAME + "/" + uploadFile.getName();

			amazonS3.putObject(bucketName, key, uploadFile);


			//임시 파일 삭제
			if(uploadFile.exists()) {
				uploadFile.delete();
			}
		}

	}
	
	// 파일 다운로드
	@Transactional
	public ResponseEntity<Resource> downloadS3File(long fileNo){
		AttachmentFile attachmentFile = null;
		Resource resource = null;
		
		// DB에서 파일 검색 -> S3의 파일 가져오기 (getObject) -> 전달
		attachmentFile = fileRepository.findById(fileNo)
				.orElseThrow(() -> new NoSuchElementException("파일 없음"));

		String key = DIR_NAME + "/" + attachmentFile.getAttachmentFileName();

		S3Object s3Object = amazonS3.getObject(bucketName, key);

		S3ObjectInputStream s3ois =  s3Object.getObjectContent();

		resource = new InputStreamResource(s3ois);


		HttpHeaders headers = new HttpHeaders();

		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

		headers.setContentDisposition(ContentDisposition
											.builder("attachment")
											.filename(attachmentFile.getAttachmentOriginalFileName())
											.build());


		return new ResponseEntity<Resource>(resource, headers, HttpStatus.OK);
	}
	
}