package lz.cim.api.jpa.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lz.cim.api.core.config.AppProperties;
import lz.cim.api.core.tool.Common;
import lz.cim.api.core.tool.DownloadTool;
import lz.cim.api.core.upload.FileUtil;
import lz.cim.api.core.upload.IoHelper;
import lz.cim.api.core.view.ReslutView;
import lz.cim.api.jpa.attchment.model.AttachmentModel;
import lz.cim.api.jpa.attchment.server.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Api(tags = "附件服务", description = "附件上传")
@RestController
@RequestMapping("/api")
public class UploadController {

    @Autowired
    AppProperties appProperties;

    @Autowired
    AttachmentService attachmentService;

    @ApiOperation("上传接口")
    @PostMapping("/bigupload")
    @ResponseBody
    public String bigUpload(@RequestParam("file") MultipartFile file, @RequestParam(value = "chunk", required = false) int chunk, @RequestParam("fileMd5") String fileMd5) {
        if (file.isEmpty()) {
            return "上传失败，请选择文件";
        }
        String savePath = appProperties.getUploadPath();
        IoHelper.judeDirExists(savePath);
        String fileName = file.getOriginalFilename();
        savePath = savePath + "\\" + fileMd5;
        IoHelper.judeDirExists(savePath);
        File dest = new File(savePath + "\\" + (chunk + ".aaa"));
        try {
            file.transferTo(dest);

            return "上传成功";
        } catch (IOException e) {

        }
        return "上传失败！";
    }

    @ApiOperation("上传接口")
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @RequestParam("fileMd5") String fileMd5) {
        ReslutView reslutView = new ReslutView();
        if (file.isEmpty()) {
            reslutView.setMsg("未选择文件");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reslutView);
        }
        String savePath = appProperties.getSavePath();
        IoHelper.judeDirExists(savePath);
        String date = Common.getStringDate();
        String fileName = file.getOriginalFilename();
        String saveName = Common.GetKey() + "." + Common.GetSuffix(fileName);
        savePath += "\\" + date;
        IoHelper.judeDirExists(savePath);
        savePath = savePath + "\\" + saveName;
        File dest = new File(savePath);
        try {
            file.transferTo(dest);
            AttachmentModel attachmentModel = getAttachmentModel(fileName, fileMd5, saveName, date);
            reslutView.setData(attachmentModel);

        } catch (IOException e) {

        }
        return ResponseEntity.status(HttpStatus.OK).body(reslutView);
    }


    @ApiOperation("检测文件是否存在")
    @PostMapping("/check")
    @ResponseBody
    public ResponseEntity<?> check(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunk") int chunk,
                                   @RequestParam("chunkSize") Integer chunkSize, @RequestParam("fileName") String fileName) throws IOException {

        ReslutView reslutView = new ReslutView();
        if (chunk == 0) {
            //第一个切片上传时判断文件是否存在
            List<AttachmentModel> attachmentModels = attachmentService.getByKey(fileMd5);
            if (attachmentModels.size() > 0) {
                AttachmentModel attachmentModel = attachmentModels.get(0);
                AttachmentModel newAttachmentModel = new AttachmentModel();
                newAttachmentModel.setId(Common.GetKey());
                newAttachmentModel.setKey(fileMd5);
                newAttachmentModel.setOldName(fileName);
                newAttachmentModel.setFileName(attachmentModel.getFileName());
                newAttachmentModel.setFilePath(attachmentModel.getFilePath());
                newAttachmentModel.setCreateTime(Common.GetDateTime());
                attachmentService.save(newAttachmentModel);
                reslutView.setData(newAttachmentModel.getId());
                reslutView.setCode("2");
                return ResponseEntity.status(HttpStatus.OK).body(reslutView);
            }
        }
        String filePath = appProperties.getUploadPath();
        filePath = filePath + "\\" + fileMd5 + "\\" + chunk + ".aaa";
        File file = new File(filePath);
        if (file.exists()) {
            if (file.length() != chunkSize) {
                reslutView.setCode("1");
            }
        } else {
            reslutView.setCode("1");
        }
        return ResponseEntity.status(HttpStatus.OK).body(reslutView);
    }


    private ReslutView saveBigFile(String fileName, String fileMd5, int chunks) throws IOException {
        ReslutView reslutView = new ReslutView();
        int blockFileSize = 1024 * 1024 * 5;
        String uploadPath = appProperties.getUploadPath() + "\\" + fileMd5;
        String saveName = Common.GetKey() + "." + Common.GetSuffix(fileName);
        String date = Common.getStringDate();

        String savePath = appProperties.getSavePath() + "\\" + date;
        IoHelper.judeDirExists(savePath);
        File d = new File(uploadPath);
        if (!d.exists()) {
            reslutView.setCode("1");
            reslutView.setMsg("未找到合并的文件");

        }
        File list[] = d.listFiles();
        int fileCount = 0;
        for (int i = 0; i < list.length; i++) {
            if (list[i].isFile()) {

                fileCount++;
            }
        }

        String filePath = savePath + "\\" + saveName;
        if (fileCount == chunks) {
            FileUtil fileUtil = new FileUtil();
            fileUtil.mergePartFiles(uploadPath, ".aaa",
                    blockFileSize, filePath);
        }
        AttachmentModel attachmentModel = getAttachmentModel(fileName, fileMd5, saveName, date);
        reslutView.setData(attachmentModel);
        return reslutView;

    }

    private AttachmentModel getAttachmentModel(String fileName, String fileMd5, String saveName, String date) {
        AttachmentModel attachmentModel = new AttachmentModel();
        attachmentModel.setId(Common.GetKey());
        attachmentModel.setKey(fileMd5);
        attachmentModel.setCreateTime(Common.GetDateTime());
        attachmentModel.setFilePath(date + "\\" + saveName);
        attachmentModel.setFileName(saveName);
        attachmentModel.setOldName(fileName);
        attachmentService.save(attachmentModel);
        return attachmentModel;
    }

    @ApiOperation("文件合并")
    @PostMapping("/merge")
    @ResponseBody
    public ResponseEntity<?> merge(@RequestParam("fileName") String fileName, @RequestParam("fileMd5") String fileMd5, @RequestParam("chunks") Integer chunks) throws IOException {

        ReslutView reslutView = new ReslutView();
        reslutView = saveBigFile(fileName, fileMd5, chunks);
        return ResponseEntity.status(HttpStatus.OK).body(reslutView);
    }

    @ApiOperation("下载文件")
    @GetMapping("/download/{id}")
    public void download(@PathVariable(name = "id") String id, HttpServletResponse httpServletResponse) {

        AttachmentModel attachmentModel = attachmentService.getById(id);
        if (attachmentModel == null) return;

        String path = appProperties.getSavePath() + "\\" + attachmentModel.getFilePath();

        DownloadTool.download(path, httpServletResponse, attachmentModel.getOldName());
    }

}
