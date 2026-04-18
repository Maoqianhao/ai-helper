package com.example.aihelper.rag.metadata;

import org.springframework.stereotype.Component;

/**
 * 元数据语义提示提供器
 *
 * 职责：集中维护表/字段相关的语义别名、业务场景与查询示例规则。
 */
@Component
public class MetadataSemanticHintProvider {

    public String inferBusinessContext(String tableName) {
        if (tableName.contains("shop")) {
            return "商铺管理（商户/商家），存储本地商铺的基本信息、位置、评分等";
        } else if (tableName.contains("user")) {
            return "用户管理，存储用户账号、个人信息等";
        } else if (tableName.contains("order")) {
            return "订单管理，存储交易订单信息";
        } else if (tableName.contains("blog")) {
            return "博客/笔记管理，存储用户发布的探店笔记";
        } else if (tableName.contains("voucher")) {
            return "优惠券管理（Voucher/Seckill/折扣/促销），存储优惠券、秒杀活动、库存等信息";
        } else if (tableName.contains("follow")) {
            return "社交关系，存储用户之间的关注关系";
        } else if (tableName.contains("sign")) {
            return "签到功能，存储用户每日签到记录";
        } else if (tableName.contains("type")) {
            return "分类管理，存储商铺或商品的分类信息";
        }
        return null;
    }

    public String generateQueryExample(TableMetadata metadata) {
        String tableName = metadata.getTableName();
        if (tableName.contains("shop")) {
            return "查询评分最高的10个商铺、查询某个商圈的所有商铺";
        } else if (tableName.contains("user")) {
            return "查询用户信息、根据手机号查找用户";
        } else if (tableName.contains("blog")) {
            return "查询某个商铺的所有探店笔记、查询点赞数最高的笔记";
        } else if (tableName.contains("voucher")) {
            return "查询某个商铺的优惠券、查询某个商铺的优惠券、查询秒杀活动的优惠券、领取优惠";
        } else if (tableName.contains("order")) {
            return "查询某个用户的订单、查询某个商铺的订单";
        }
        return null;
    }

    public String[] inferTableAliases(String tableName) {
        if (tableName.equals("tb_shop")) {
            return new String[]{"商铺", "商家", "shop", "store", "商户"};
        } else if (tableName.equals("tb_user")) {
            return new String[]{"用户", "user", "账号", "账户"};
        } else if (tableName.equals("tb_user_info")) {
            return new String[]{"用户信息", "user_info", "个人信息", "资料"};
        } else if (tableName.equals("tb_voucher")) {
            return new String[]{"优惠券", "券", "voucher", "coupon", "代金券"};
        } else if (tableName.equals("tb_voucher_order")) {
            return new String[]{"订单", "order", "购买记录", "交易"};
        } else if (tableName.equals("tb_seckill_voucher")) {
            return new String[]{"秒杀券", "seckill", "限时抢购"};
        } else if (tableName.equals("tb_blog")) {
            return new String[]{"笔记", "博客", "blog", "探店笔记", "文章"};
        } else if (tableName.equals("tb_blog_comments")) {
            return new String[]{"评论", "comments", "留言"};
        } else if (tableName.equals("tb_follow")) {
            return new String[]{"关注", "follow", "粉丝", "社交"};
        } else if (tableName.equals("tb_sign")) {
            return new String[]{"签到", "sign", "打卡"};
        } else if (tableName.equals("tb_shop_type")) {
            return new String[]{"商铺类型", "分类", "type", "category"};
        }
        return null;
    }

    public String[] inferFieldEnglishKeywords(String fieldName, String comment) {
        if (fieldName.equals("x")) {
            return new String[]{"经度", "longitude", "lng", "坐标"};
        } else if (fieldName.equals("y")) {
            return new String[]{"纬度", "latitude", "lat", "坐标"};
        } else if (fieldName.equals("phone")) {
            return new String[]{"手机", "电话", "mobile", "tel"};
        } else if (fieldName.equals("nick_name")) {
            return new String[]{"昵称", "nickname", "用户名"};
        } else if (fieldName.equals("password")) {
            return new String[]{"密码", "pwd", "口令"};
        } else if (fieldName.equals("icon")) {
            return new String[]{"头像", "avatar", "图片"};
        } else if (fieldName.equals("score")) {
            return new String[]{"评分", "rating", "分数"};
        } else if (fieldName.equals("sold")) {
            return new String[]{"销量", "sales", "销售量"};
        } else if (fieldName.equals("comments")) {
            return new String[]{"评论数", "评价数", "评论数量"};
        } else if (fieldName.equals("avg_price")) {
            return new String[]{"均价", "平均价格", "price"};
        } else if (fieldName.equals("address")) {
            return new String[]{"地址", "location", "详细地址"};
        } else if (fieldName.equals("area")) {
            return new String[]{"商圈", "区域", "district"};
        } else if (fieldName.equals("open_hours")) {
            return new String[]{"营业时间", "hours", "工作时间"};
        } else if (fieldName.equals("stock")) {
            return new String[]{"库存", "inventory", "剩余数量"};
        } else if (fieldName.equals("title")) {
            return new String[]{"标题", "名称", "name"};
        } else if (fieldName.equals("content")) {
            return new String[]{"内容", "正文", "描述"};
        } else if (fieldName.equals("images")) {
            return new String[]{"图片", "照片", "img", "image"};
        } else if (fieldName.equals("liked")) {
            return new String[]{"点赞", "like", "喜欢"};
        } else if (fieldName.equals("fans")) {
            return new String[]{"粉丝", "follower", "关注者"};
        } else if (fieldName.equals("followee")) {
            return new String[]{"关注数", "following", "关注的人"};
        } else if (fieldName.equals("gender")) {
            return new String[]{"性别", "sex", "男女"};
        } else if (fieldName.equals("birthday")) {
            return new String[]{"生日", "birth", "出生日期"};
        } else if (fieldName.equals("credits")) {
            return new String[]{"积分", "points", "credit"};
        } else if (fieldName.equals("level")) {
            return new String[]{"等级", "级别", "会员等级"};
        } else if (fieldName.equals("status")) {
            return new String[]{"状态", "status"};
        } else if (fieldName.equals("create_time")) {
            return new String[]{"创建时间", "created", "建立时间"};
        } else if (fieldName.equals("update_time")) {
            return new String[]{"更新时间", "updated", "修改时间"};
        }
        return null;
    }
}
