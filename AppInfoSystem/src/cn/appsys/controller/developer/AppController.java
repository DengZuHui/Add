package cn.appsys.controller.developer;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONArray;
import com.mysql.jdbc.StringUtils;
import com.sun.org.apache.xpath.internal.operations.Mod;
import com.sun.scenario.effect.impl.prism.PrReflectionPeer;

import cn.appsys.pojo.AppCategory;
import cn.appsys.pojo.AppInfo;
import cn.appsys.pojo.AppVersion;
import cn.appsys.pojo.DataDictionary;
import cn.appsys.pojo.DevUser;
import cn.appsys.service.developer.AppCategoryService;
import cn.appsys.service.developer.AppInfoService;
import cn.appsys.service.developer.AppVersionService;
import cn.appsys.service.developer.DataDictionaryService;
import cn.appsys.tools.Constants;
import cn.appsys.tools.PageSupport;

@Controller
@RequestMapping("/dev/flatform/app")
public class AppController {
	@Resource
	private AppInfoService appInfoService;
	@Resource
	private DataDictionaryService dataDictionaryService;
	@Resource
	private AppCategoryService appCategoryService;
	@Resource
	private AppVersionService appVersionService;

	@RequestMapping("/list")
	public String getAppInfoList(Model model,HttpSession session,
			@RequestParam(value="querySoftwareName",required=false) String querySoftwareName,
			@RequestParam(value="queryStatus",required=false) String _queryStatus,
			@RequestParam(value="queryCategoryLevel1",required=false) String _queryCategoryLevel1,
			@RequestParam(value="queryCategoryLevel2",required=false) String _queryCategoryLevel2,
			@RequestParam(value="queryCategoryLevel3",required=false) String _queryCategoryLevel3,
			@RequestParam(value="queryFlatformId",required=false) String _queryFlatformId,
			@RequestParam(value="pageIndex",required=false) String pageIndex) throws Exception {
		Integer devId = ((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId();
		List<AppInfo> appInfoList = null; //获取所有app信息
		List<DataDictionary> statusList = null;	//获取APP的状态
		List<DataDictionary> flatFormList = null;
		List<AppCategory> categoryLevel1List = null;//列出一级分类列表，注：二级和三级分类列表通过异步ajax获取
		List<AppCategory> categoryLevel2List = null;
		List<AppCategory> categoryLevel3List = null;
		//页面容量
		int pageSize = Constants.pageSize;
		//当前页码
		Integer currentPageNo = 1;
		if (pageIndex != null) {
			try {
				currentPageNo = Integer.valueOf(pageIndex);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Integer queryStatus = null;
		if(_queryStatus != null && !_queryStatus.equals("")) {
			queryStatus = Integer.parseInt(_queryStatus);
		}
		Integer queryCategoryLevel1 = null;
		if(_queryCategoryLevel1 != null && !_queryCategoryLevel1.equals("")) {
			queryCategoryLevel1 = Integer.parseInt(_queryCategoryLevel1);
		}
		Integer queryCategoryLevel2 = null;
		if(_queryCategoryLevel2 != null && !_queryCategoryLevel2.equals("")) {
			queryCategoryLevel2 = Integer.parseInt(_queryCategoryLevel2);
		}
		Integer queryCategoryLevel3 = null;
		if(_queryCategoryLevel3 != null && !_queryCategoryLevel3.equals("")) {
			queryCategoryLevel3 = Integer.parseInt(_queryCategoryLevel3);
		}
		Integer queryFlatformId = null;
		if(_queryFlatformId != null && !_queryFlatformId.equals("")) {
			queryFlatformId = Integer.parseInt(_queryFlatformId);
		}
		//总数量
		int totalCount = 0;
		totalCount = appInfoService.getAppInfoCount(querySoftwareName, queryStatus, queryCategoryLevel1, queryCategoryLevel2, queryCategoryLevel3, queryFlatformId, devId);
		//总页数
		PageSupport pages = new PageSupport();
		pages.setCurrentPageNo(currentPageNo);
		pages.setPageSize(pageSize);
		pages.setTotalCount(totalCount);
		int totalPageCount = pages.getTotalPageCount();
		//控制首页和尾页
		if(currentPageNo < 1){
			currentPageNo = 1;
		}else if(currentPageNo > totalPageCount){
			currentPageNo = totalPageCount;
		}
		appInfoList = appInfoService.getAppInfoList(querySoftwareName, queryStatus, queryCategoryLevel1, queryCategoryLevel2, queryCategoryLevel3, queryFlatformId, devId, currentPageNo, pageSize);
		statusList = getDataDictionaryList("APP_STATUS");
		flatFormList = getDataDictionaryList("APP_FLATFORM");
		categoryLevel1List = appCategoryService.getAppCategoryListByParentId(null);
		model.addAttribute("appInfoList", appInfoList);
		model.addAttribute("statusList", statusList);
		model.addAttribute("flatFormList", flatFormList);
		model.addAttribute("categoryLevel1List",categoryLevel1List);
		model.addAttribute("pages", pages);
		model.addAttribute("queryStatus", queryStatus);
		model.addAttribute("queryCategoryLevel1", queryCategoryLevel1);
		model.addAttribute("queryCategoryLevel2", queryCategoryLevel2);
		model.addAttribute("queryCategoryLevel3", queryCategoryLevel3);
		model.addAttribute("queryFlatformId", queryFlatformId);

		//二级分类列表和三级分类列表---回显
		if(queryCategoryLevel2 != null && !queryCategoryLevel2.equals("")){
			categoryLevel2List = getCategoryList(queryCategoryLevel1.toString());
			model.addAttribute("categoryLevel2List", categoryLevel2List);
		}
		if(queryCategoryLevel3 != null && !queryCategoryLevel3.equals("")){
			categoryLevel3List = getCategoryList(queryCategoryLevel2.toString());
			model.addAttribute("categoryLevel3List", categoryLevel3List);
		}
		return "developer/appinfolist";
	}
	/**
	 * 退出登录
	 * @param session
	 * @return
	 */
	@RequestMapping("/logout")
	public String logout(HttpSession session)
	{
		session.removeAttribute("userSession");
		return "backendlogin";
	}
	/**
	 * 添加方法
	 * @param appInfo
	 * @param session
	 * @param request
	 * @param attach
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value="/appinfoaddsave",method=RequestMethod.POST)
	public String add(AppInfo appInfo,HttpSession session,HttpServletRequest request,@RequestParam(value="a_logoPicPath",required= false) MultipartFile attach) throws Exception {
		String logoPicPath =  null;
		String logoLocPath =  null;	
		String APKName = appInfo.getAPKName();
		if(!attach.isEmpty()){
			String path = request.getSession().getServletContext().getRealPath("statics"+File.separator+"uploadfiles");
			String oldFileName = attach.getOriginalFilename();//原文件名
			String prefix = FilenameUtils.getExtension(oldFileName);//原文件后缀
			int filesize = 500000;
			if(attach.getSize() > filesize){//上传大小不得超过 50k
				return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
				+"&error=error4";
			}else if(prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("png") 
					||prefix.equalsIgnoreCase("jepg") || prefix.equalsIgnoreCase("pneg")){//上传图片格式
				String fileName = APKName + ".jpg";//上传LOGO图片命名:apk名称.apk
				File targetFile = new File(path,fileName);
				if(!targetFile.exists()){
					targetFile.mkdirs();
				}
				try {
					attach.transferTo(targetFile);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
					+"&error=error2";
				} 
				logoPicPath = request.getContextPath()+"/statics/uploadfiles/"+fileName;
				logoLocPath = path+File.separator+fileName;
			}else{
				return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
				+"&error=error3";
			}
			appInfo.setCreatedBy(((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId());
			appInfo.setCreationDate(new Date());
			appInfo.setLogoLocPath(logoLocPath);
			appInfo.setLogoPicPath(logoPicPath);
			appInfo.setDevId(((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId());
			appInfo.setStatus(1);
			try {
				if (appInfoService.add(appInfo)) {
					return "redirect:/dev/flatform/app/list";
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return "developer/appinfoadd";
	}
	
	/**
	 * 增加app信息跳转页面
	 * @param appInfo
	 * @return
	 */
	@RequestMapping(value="/appinfoadd",method=RequestMethod.GET)
	public String add(@ModelAttribute("appInfo") AppInfo appInfo){
		return "developer/appinfoadd";
	}
	
	/**
	 * 根据typeCode查询出相应的数据字典列表
	 * @param pid
	 * @return
	 */
	public List<AppCategory> getCategoryList(String pid){
		List<AppCategory> categoryLevelList = null;
		try {
			categoryLevelList = appCategoryService.getAppCategoryListByParentId(pid==null?null:Integer.parseInt(pid));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return categoryLevelList;
	}
	/**
	 * 根据parentId查询出相应的分类级别列表
	 * @param pid
	 * @return
	 * @throws Exception 
	 * @throws NumberFormatException 
	 */
	@RequestMapping(value="/categorylevellist.json",method=RequestMethod.GET)
	@ResponseBody
	public List<AppCategory> getAppCategoryList (@RequestParam String pid) throws NumberFormatException, Exception{
		if(pid.equals("")) pid = null;
		return getCategoryList(pid);
	}
	
	/**
	 * 根据typeCode查询出相应的数据字典列表
	 * @param pid
	 * @return
	 */
	public List<DataDictionary> getDataDictionaryList(String typeCode){
		List<DataDictionary> dataDictionaryList = null;
		try {
			dataDictionaryList = dataDictionaryService.getDataDictionaryList(typeCode);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dataDictionaryList;
	}
	
	/**
	 * 通过ajax获得所属平台
	 * @param tcode
	 * @return
	 */
	@RequestMapping("datadictionarylist")
	@ResponseBody
	public Object getDataDictionary(String tcode) {
		List<DataDictionary> dataDictionaryList = null;
		try {
			dataDictionaryList = dataDictionaryService.getDataDictionaryList(tcode);
		} catch (Exception e) {
			// TODO 自动生成的 catch 块
			e.printStackTrace();
		}
		return JSONArray.toJSON(dataDictionaryList);
	}
	
	/**
	 * 修改appInfo信息跳转页面
	 * @param id
	 * @param model
	 * @return
	 */
	@RequestMapping(value="/appinfomodify",method=RequestMethod.GET)
	public String modifyAppInfo(@RequestParam("id") String id,@RequestParam(value="error",required= false)String fileUploadError,Model model){
		AppInfo appInfo = null;
		if (null != fileUploadError && fileUploadError.equals("error1")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_1;
		} else if (null != fileUploadError && fileUploadError.equals("error2")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_2;
		} else if (null != fileUploadError && fileUploadError.equals("error3")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_3;
		} else if (null != fileUploadError && fileUploadError.equals("error4")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_4;
		}
		try {
			appInfo = appInfoService.getAppInfo(Integer.parseInt(id), null);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		model.addAttribute(appInfo);
		model.addAttribute("fileUploadError",fileUploadError);
		return "developer/appinfomodify";
	}
	
	/**
	 * 保存修改后的appInfo
	 * @param appInfo
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/appinfomodifysave",method=RequestMethod.POST)
	public String modifySave(AppInfo appInfo,HttpSession session,HttpServletRequest request,@RequestParam(value="attach",required= false) MultipartFile attach){
		String logoPicPath =  null;
		String logoLocPath =  null;
		String APKName = appInfo.getAPKName();
		if(!attach.isEmpty()){
			String path = request.getSession().getServletContext().getRealPath("statics"+File.separator+"uploadfiles"); 
			String oldFileName = attach.getOriginalFilename();//原文件名
			String prefix = FilenameUtils.getExtension(oldFileName);//原文件后缀
			int filesize = 500000;
			if(attach.getSize() > filesize){//上传大小不得超过 50k
				return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
				+"&error=error4";
			}else if(prefix.equalsIgnoreCase("jpg") || prefix.equalsIgnoreCase("png") 
					||prefix.equalsIgnoreCase("jepg") || prefix.equalsIgnoreCase("pneg")){//上传图片格式
				String fileName = APKName + ".jpg";//上传LOGO图片命名:apk名称.apk
				File targetFile = new File(path,fileName);
				if(!targetFile.exists()){
					targetFile.mkdirs();
				}
				try {
					attach.transferTo(targetFile);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
					+"&error=error2";
				} 
				logoPicPath = request.getContextPath()+"/statics/uploadfiles/"+fileName;
				logoLocPath = path+File.separator+fileName;
			}else{
				return "redirect:/dev/flatform/app/appinfomodify?id="+appInfo.getId()
				+"&error=error3";
			}
		}
		appInfo.setModifyBy(((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId());
		appInfo.setModifyDate(new Date());
		appInfo.setLogoLocPath(logoLocPath);
		appInfo.setLogoPicPath(logoPicPath);
		try {
			if(appInfoService.modify(appInfo)) {
				return "redirect:/dev/flatform/app/list";
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "developer/appinfomodify";
	}
	
	/**
	 * 删除app信息
	 * @param id
	 * @return
	 */
	@RequestMapping(value="/delapp.json")
	@ResponseBody
	public Object delApp(@RequestParam String id){
		HashMap<String, String> resultMap = new HashMap<String,String>();
		if (StringUtils.isNullOrEmpty(id)) {
			resultMap.put("delResult", "notexist");
		} else {
			try {
				if(appInfoService.appsysdeleteAppById(Integer.parseInt(id))) {
					resultMap.put("delResult","true");
				} else {
					resultMap.put("delResult", "false");
				}
			}catch (NumberFormatException e) {
				// TODO: handle exception
				e.printStackTrace();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return JSONArray.toJSONString(resultMap);
	}
	
	/**
	 * 查看app信息，app基本信息和版本信息列表
	 * @param appInfo
	 * @return
	 */
	@RequestMapping(value="/appview/{id}",method=RequestMethod.GET)
	public String view(@PathVariable String id,Model model) {
		AppInfo appInfo = null;
		List<AppVersion> appVersionsList = null;
		try {
			appInfo = appInfoService.getAppInfo(Integer.parseInt(id), null);
			appVersionsList = appVersionService.getAppVersionList(Integer.parseInt(id));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		model.addAttribute("appVersionsList", appVersionsList);
		model.addAttribute(appInfo);
		return "developer/appinfoview";
	}
	
	/**
	 * 保存修改后的appVersion
	 * @param appVersion
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/appversionmodifysave",method=RequestMethod.POST)
	public String modifyAppVersionSave(AppVersion appVersion,HttpSession session,HttpServletRequest request,
			@RequestParam(value="attach",required= false) MultipartFile attach){	
		String downloadLink =  null;
		String apkLocPath = null;
		String apkFileName = null;
		if (!attach.isEmpty()) {
			String path = request.getSession().getServletContext().getRealPath("statics"+File.separator+"uploadfiles");
			String oldFileName = attach.getOriginalFilename();	//原文件名
			String prefix = FilenameUtils.getExtension(oldFileName);	//原文件后缀
			if(prefix.equalsIgnoreCase("apk")) {	//apk文件命名：apk名称+版本号+.apk
				String apkName = null;
				try {
					apkName = appInfoService.getAppInfo(appVersion.getAppId(), null).getAPKName();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				if (apkName == null || "".equals(apkName)) {
					return "redirect:/dev/flatform/app/appversionmodify?vid="+appVersion.getId() +"&aid="+appVersion.getAppId() +"&error=error1";
				} 
				apkFileName = apkName + "-" + appVersion.getVersionNo() + ".apk";
				File targetFile = new File(path,apkFileName);
				if(!targetFile.exists()) {
					targetFile.mkdirs();
				}
				try {
					attach.transferTo(targetFile);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					return "redirect:/dev/flatform/app/appversionmodify?vid="+appVersion.getId() +"&aid="+appVersion.getAppId() +"&error=error2";
				}
				downloadLink = request.getContextPath()+"/statics/uploadfiles/"+apkFileName;
				apkLocPath = path + File.separator+apkFileName;
			} else {
				return "redirect:/dev/flatform/app/appversionmodify?vid="+appVersion.getId() +"&aid="+appVersion.getAppId() +"&error=error3";
			}
		}
		appVersion.setModifyBy(((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId());
		appVersion.setModifyDate(new Date());
		appVersion.setDownloadLink(downloadLink);
		appVersion.setApkLocPath(apkLocPath);
		appVersion.setApkFileName(apkFileName);
		try {
			if(appVersionService.modify(appVersion)) {
				return "redirect:/dev/flatform/app/list";
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "developer/appversionmodify";
	}

	/**
	 * 保存修改后的appVersion
	 * @param appVersion
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/appversionmodify",method=RequestMethod.GET)
	public String modifyAppVersion(@RequestParam("vid") String versionId,@RequestParam("aid") String appId,@RequestParam(value="error",required= false)String fileUploadError,Model model){
		AppVersion appVersion = null;
		List<AppVersion> appVersionList = null;
		if (null != fileUploadError && fileUploadError.equals("error1")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_1;
		} else if (null != fileUploadError && fileUploadError.equals("error2")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_2;
		} else if (null != fileUploadError && fileUploadError.equals("error3")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_3;
		}
		try {
			appVersion = appVersionService.getAppVersionById(Integer.parseInt(versionId));
			appVersionList = appVersionService.getAppVersionList(Integer.parseInt(appId));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		model.addAttribute(appVersion);
		model.addAttribute("appVersionList",appVersionList);
		model.addAttribute("fileUploadError",fileUploadError);
		return "developer/appversionmodify";
	}
	
	/**
	 * 保存新增appversion数据（子表）-上传该版本的apk包
	 * @param appInfo
	 * @param appVersion
	 * @param session
	 * @param request
	 * @param attach
	 * @return
	 */
	@RequestMapping(value="/appversionadd",method=RequestMethod.GET)
	public String addVersion(@RequestParam(value="id")String appId,@RequestParam(value="error",required= false)String fileUploadError,AppVersion appVersion,Model model){
		if (null != fileUploadError && fileUploadError.equals("error1")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_1;
		} else if (null != fileUploadError && fileUploadError.equals("error2")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_2;
		} else if (null != fileUploadError && fileUploadError.equals("error3")) {
			fileUploadError = Constants.FILEUPLOAD_ERROR_3;
		}
		appVersion.setAppId(Integer.parseInt(appId));
		List<AppVersion> appVersionList = null;
		try {
			appVersionList = appVersionService.getAppVersionList(Integer.parseInt(appId));
			appVersion.setAppName((appInfoService.getAppInfo(Integer.parseInt(appId),null)).getSoftwareName());
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		model.addAttribute(appVersion);
		model.addAttribute("appVersionList",appVersionList);
		model.addAttribute("fileUploadError",fileUploadError);
		return "developer/appversionadd";
	}
	
	/**
	 * 保存新增appversion数据（子表）-上传该版本的apk包
	 * @param appInfo
	 * @param appVersion
	 * @param session
	 * @param request
	 * @param attach
	 * @return
	 */
	@RequestMapping(value="/addversionsave",method=RequestMethod.POST)
	public String addVersionSave(AppVersion appVersion,HttpSession session,HttpServletRequest request,@RequestParam(value="a_downloadLink",required= false) MultipartFile attach ){
		String downloadLink =  null;
		String apkLocPath = null;
		String apkFileName = null;
		if(!attach.isEmpty()) {
			String path = request.getSession().getServletContext().getRealPath("statics"+File.separator+"uploadfiles");
			String oldFileName = attach.getOriginalFilename();
			String prefix = FilenameUtils.getExtension(oldFileName);
			if(prefix.equalsIgnoreCase("apk")) {
				String apkName = null;
				try {
					apkName = appInfoService.getAppInfo(appVersion.getAppId(),null).getAPKName();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
				if (apkName == "null" || "".equals(apkName)) {
					return "redirect:/dev/flatform/app/appversionadd?id="+appVersion.getAppId() +"&error=error1";
				}
				apkFileName = apkName + "-" + appVersion.getVersionNo() + ".apk";
				File targeFile = new File(path,apkFileName);
				if(!targeFile.exists()) {
					targeFile.mkdirs();
				}
				try {
					attach.transferTo(targeFile);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
					return "redirect:/dev/flatform/app/appversionadd?id="+appVersion.getAppId() +"&error=error2";
				}
				downloadLink = request.getContextPath() + "/statics/uploadfiles/" + apkFileName;
				apkLocPath = path + File.separator + apkFileName;
			} else {
				return "redirect:/dev/flatform/app/appversionadd?id="+appVersion.getAppId() +"&error=error3";
			}
		}
		appVersion.setCreatedBy(((DevUser)session.getAttribute(Constants.DEV_USER_SESSION)).getId());
		appVersion.setCreationDate(new Date());
		appVersion.setDownloadLink(downloadLink);
		appVersion.setApkLocPath(apkLocPath);
		appVersion.setApkFileName(apkFileName);
		try {
			if (appVersionService.appsysadd(appVersion)) {
				return "redirect:/dev/flatform/app/list";
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return "redirect:/dev/flatform/app/appversionadd?id="+appVersion.getAppId();
	}
	
	/**
	 * 修改操作时，删除图片或文件，并更新数据库
	 * @param fileUrlPath
	 * @param fileLocPath
	 * @param flag
	 * @param id
	 * @return
	 */
	@RequestMapping(value ="/delfile",method=RequestMethod.GET)
	@ResponseBody
	public Object delFile(@RequestParam(value="flag",required=false) String flag, @RequestParam(value="id",required=false) String id){
		HashMap<String, String> resultMap = new HashMap<String, String>();
		String fileLocPath = null;
		if(flag == null || flag.equals("") ||
			id == null || id.equals("")){
			resultMap.put("result", "failed");
		}else if(flag.equals("logo")){//删除logo图片（操作app_info）
			try {
				fileLocPath = (appInfoService.getAppInfo(Integer.parseInt(id), null)).getLogoLocPath();
				File file = new File(fileLocPath);
			    if(file.exists())
			     if(file.delete()){//删除服务器存储的物理文件
						if(appInfoService.deleteAppLogo(Integer.parseInt(id))){//更新表
							resultMap.put("result", "success");
						 }
			    }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else if(flag.equals("apk")){//删除apk文件（操作app_version）
			try {
				fileLocPath = (appVersionService.getAppVersionById(Integer.parseInt(id))).getApkLocPath();
				File file = new File(fileLocPath);
			    if(file.exists())
			     if(file.delete()){//删除服务器存储的物理文件
						if(appVersionService.deleteApkFile(Integer.parseInt(id))){//更新表
							resultMap.put("result", "success");
						 }
			    }
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return JSONArray.toJSONString(resultMap);
	}
	
	/**
	 * 通过Id修改上架和下架
	 * @param appid
	 * @param session
	 * @return
	 */
	@RequestMapping(value="/{appid}/sale",method=RequestMethod.PUT)
	@ResponseBody
	public Object sale(@PathVariable String appid,HttpSession session){
		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		Integer appIdInteger = 0;
		try {
			appIdInteger = Integer.parseInt(appid);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		resultMap.put("errorCode", "0");
		resultMap.put("appId", appid);
		if (appIdInteger > 0) {
			try {
				DevUser devUser = (DevUser)session.getAttribute(Constants.DEV_USER_SESSION);
				AppInfo appInfo = new AppInfo();
				appInfo.setId(appIdInteger);
				appInfo.setModifyBy(devUser.getId());
				if(appInfoService.appsysUpdateSaleStatusByAppId(appInfo)) {
					resultMap.put("resultMsg", "success");
				} else {
					resultMap.put("resultMsg", "success");
				}
			} catch (Exception e) {
				resultMap.put("errorCode", "exception000001");
			}
		} else {
			resultMap.put("errorCode", "param000001");
		}
		return resultMap;
	}
}