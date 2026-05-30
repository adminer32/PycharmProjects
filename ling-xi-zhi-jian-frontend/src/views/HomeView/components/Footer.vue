<script setup lang="ts">
import { useI18n } from 'vue-i18n';

const { t } = useI18n();
</script>

<template>
    <footer class="footer">
        <div class="footer-content">
            <div class="footer-brand">
                <div class="logo">
                    <img src="/favicon.png" alt="" />
                    <span class="site-title">{{ $t('site.title') }}</span>
                </div>
                <p class="brand-slogan">翎翼毽球，你网页上的专属踢毽伙伴</p>
            </div>

            <div class="footer-links">
                <div class="link-group">
                    <div class="link-title">关于我们</div>
                    <div class="link-items">
                        <RouterLink to="#" class="menu-item">成长记忆</RouterLink>
                        <RouterLink to="#" class="menu-item">工作机会</RouterLink>
                        <RouterLink to="#" class="menu-item">联系我们</RouterLink>
                    </div>
                </div>

                <div class="link-group">
                    <div class="link-title">网站服务</div>
                    <div class="link-items">
                        <RouterLink :to="{ name: 'homeView' }" class="menu-item">课程中心</RouterLink>
                        <RouterLink :to="{ name: 'homeView' }" class="menu-item">AI学习伴侣</RouterLink>
                        <RouterLink :to="{ name: 'homeView' }" class="menu-item">3D动作演示</RouterLink>
                        <RouterLink :to="{ name: 'homeView' }" class="menu-item">动作评估</RouterLink>
                        <RouterLink :to="{ name: 'homeView' }" class="menu-item">个人评估</RouterLink>
                    </div>
                </div>

                <div class="link-group">
                    <div class="link-title">友情链接</div>
                    <div class="link-items">
                        <a target="_blank" href="https://tyj.gd.gov.cn" class="menu-item">广东省体育局</a>
                        <a target="_blank" href="http://jianqiu.sport.org.cn" class="menu-item">中国毽球协会</a>
                        <a target="_blank" href="https://www.sport.gov.cn" class="menu-item">国家体育总局</a>
                    </div>
                </div>

                <div class="link-group">
                    <div class="link-title">法律条款</div>
                    <div class="link-items">
                        <RouterLink to="#" class="menu-item">版权声明</RouterLink>
                        <RouterLink to="#" class="menu-item">隐私条款</RouterLink>
                        <RouterLink to="#" class="menu-item">意见反馈</RouterLink>
                    </div>
                </div>
            </div>
        </div>

        <div class="footer-bottom">
            <div class="divider"></div>
            <div class="bottom-content">
                <span class="copyright">{{ t('footer.copyright') }}</span>
            </div>
        </div>
    </footer>
</template>

<style scoped lang="scss">
.footer {
    background-color: #fafbfc;
    color: #333;
    padding: 60px 80px 30px;
    font-size: 14px;

    .footer-content {
        display: flex;
        justify-content: space-between;
        max-width: 1400px;
        margin: 0 auto;
        gap: 80px;
    }

    .footer-brand {
        display: flex;
        flex-direction: column;
        gap: 16px;
        max-width: 280px;

        .logo {
            display: flex;
            align-items: center;
            gap: 12px;

            img {
                $size: 48px;
                height: $size;
                width: $size;
                border-radius: 12px;
                box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
            }

            .site-title {
                font-size: 1.4rem;
                font-weight: 600;
                color: #1a1a1a;
                letter-spacing: 0.5px;
            }
        }

        .brand-slogan {
            font-size: 13px;
            color: #666;
            line-height: 1.6;
            margin: 0;
        }
    }

    .footer-links {
        display: flex;
        gap: 60px;
        flex: 1;
        justify-content: flex-end;

        .link-group {
            min-width: 120px;

            .link-title {
                font-size: 15px;
                font-weight: 600;
                color: #1a1a1a;
                margin-bottom: 16px;
                letter-spacing: 0.3px;
            }

            .link-items {
                display: flex;
                flex-direction: column;
                gap: 10px;

                a {
                    color: #666;
                    text-decoration: none;
                    transition: color 0.2s ease;
                    font-size: 13px;

                    &:hover {
                        color: $blue-9;
                    }
                }
            }
        }
    }

    .footer-bottom {
        max-width: 1400px;
        margin: 50px auto 0;

        .divider {
            height: 1px;
            background: linear-gradient(90deg, transparent, #e0e0e0 20%, #e0e0e0 80%, transparent);
            margin-bottom: 24px;
        }

        .bottom-content {
            display: flex;
            justify-content: center;

            .copyright {
                font-size: 13px;
                color: #999;
                letter-spacing: 0.5px;
            }
        }
    }
}
</style>
