package com.xin.xinpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xin.xinpicturebackend.exception.BusinessException;
import com.xin.xinpicturebackend.exception.ErrorCode;
import com.xin.xinpicturebackend.exception.ThrowUtils;
import com.xin.xinpicturebackend.mapper.SpaceMapper;
import com.xin.xinpicturebackend.model.dto.space.analyze.*;
import com.xin.xinpicturebackend.model.entity.Picture;
import com.xin.xinpicturebackend.model.entity.Space;
import com.xin.xinpicturebackend.model.entity.User;
import com.xin.xinpicturebackend.model.enums.SizeRangeEnum;
import com.xin.xinpicturebackend.model.vo.space.analyze.*;
import com.xin.xinpicturebackend.service.PictureService;
import com.xin.xinpicturebackend.service.SpaceAnalyzeService;
import com.xin.xinpicturebackend.service.SpaceService;
import com.xin.xinpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceAnalyzeService {

    @Resource
    UserService userService;

    @Resource
    SpaceService spaceService;

    @Resource
    PictureService pictureService;

    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();

        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else {
            // 分析特定空间的图片，仅本人或管理员可访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == 0, ErrorCode.PARAMS_ERROR);
            //校验空间是否存在
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
            //空间存在，校验权限
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        ThrowUtils.throwIf(spaceAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        //全空间分析
        if (queryAll) {
            return;
        }
        //公共图库
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        //分析特定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != 0) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围！");
    }

    /**
     * 获取空间使用情况分析
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        //参数校验
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        boolean queryPublic = spaceUsageAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceUsageAnalyzeRequest.isQueryAll();
        //全空间查询或公共图库，需要从 Picture 表中查询
        if (queryPublic || queryAll) {
            //权限校验
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //统计图库的使用空间
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            //补充查询范围
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            //查表 (只查询指定字段)
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(obj -> Long.parseLong(obj.toString())).sum();
            long usedCount = pictureObjList.size();
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            //分析公共图库的话，以下字段为空
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            return spaceUsageAnalyzeResponse;
        } else {
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == 0, ErrorCode.PARAMS_ERROR);
            //分析特定空间的图库
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在！");
            //校验权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            //封装返回结果
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            //计算比例
            double sizeUsageRatio = NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(),2).doubleValue();
            double countUsageRatio = NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(),2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //校验权限
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        //分组查询
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize")
                .groupBy("category");
        //查询
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(res -> {
                    String category = res.get("category") != null ? res.get("category").toString() : "未分类";
                    Long count = ((Number)res.get("count")).longValue();
                    Long totalSize = ((Number)res.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        //查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjectUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        //解析标签并统计
        Map<String, Long> tagCountMap = tagJsonList.stream()
                .flatMap(tagJson -> JSONUtil.toList(tagJson, String.class).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        //将结果进行排序（使用次数）
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()) )
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间图片大小分析
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        //校验权限
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

        //查询所有符合条件的图片大小
        queryWrapper.select("picSize");
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjectUtil::isNotNull)
                .map(size -> Long.parseLong(size.toString()))
                .collect(Collectors.toList());
        //定义分段范围，使用有序的Map
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<"+ SizeRangeEnum.RANGE_1.getSizeRange(), picSizeList.stream().filter(size -> size < SizeRangeEnum.RANGE_1.getSize()).count());
        sizeRanges.put(SizeRangeEnum.RANGE_1.getSizeRange()+"-"+SizeRangeEnum.RANGE_2.getSizeRange(), picSizeList.stream().filter(size -> size >= SizeRangeEnum.RANGE_1.getSize() && size < SizeRangeEnum.RANGE_2.getSize()).count());
        sizeRanges.put(SizeRangeEnum.RANGE_2.getSizeRange()+"-"+SizeRangeEnum.RANGE_3.getSizeRange(), picSizeList.stream().filter(size -> size >= SizeRangeEnum.RANGE_2.getSize() && size < SizeRangeEnum.RANGE_3.getSize()).count());
        sizeRanges.put(">"+SizeRangeEnum.RANGE_3.getSizeRange(), picSizeList.stream().filter(size -> size >= SizeRangeEnum.RANGE_3.getSize()).count());

        //转换为响应对象
        return sizeRanges.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间用户上传行为分析
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        //校验参数
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        //构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        //补充用户 id 查询
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotEmpty(userId),"userId", userId);

        //补充分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as period", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as period", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as period", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度错误");
        }

        //分组排序
        queryWrapper.groupBy("period").orderByAsc("period");

        //封装返回结果
        List<Map<String, Object>> queryRes = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryRes.stream()
                .map(map -> new SpaceUserAnalyzeResponse(map.get("period").toString(), ((Number)map.get("count")).longValue()))
                .collect(Collectors.toList());
    }

    /**
     * 获取空间排行榜（仅管理员）
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        //参数校验的
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        //权限校验
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        //构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("limit "+spaceRankAnalyzeRequest.getTopN());

        //封装封装结果
        return spaceService.list(queryWrapper);
    }
}
