<template>
    <div class="video-upload">
        <div class="mode-tabs">
            <div
                class="tab"
                :class="{ active: mode == 'file' }"
                @click="mode = 'file'"
            >
                <span class="iconfont icon-shipinshangchuan"></span>
                <span>视频上传</span>
            </div>
            <div
                class="tab"
                :class="{ active: mode == 'link' }"
                @click="mode = 'link'"
            >
                <span class="iconfont icon-lianjie"></span>
                <span>链接上传</span>
            </div>
        </div>

        <div
            class="drop-area"
            v-if="mode === 'file'"
            @dragover.prevent
            @click="openFileSelectWindow"
            @drop.prevent="handleDrop"
        >
            <template v-if="!selectedFile">
                <div class="upload-icon">
                    <span class="iconfont icon-shipinshangchuan"></span>
                </div>
                <div class="upload-text">
                    <h4>点击上传视频</h4>
                    <p>或拖拽视频文件到此处</p>
                </div>
                <div class="formats">
                    <span>支持 MP4、AVI、MOV 格式</span>
                </div>
            </template>
            <template v-else>
                <div class="file-selected" v-if="!aiStore.isVideoUploading">
                    <div class="file-icon">
                        <span class="iconfont icon-shipinwenjian"></span>
                    </div>
                    <div class="file-info">
                        <span class="file-name">{{ selectedFile.name }}</span>
                        <span class="file-size">{{ formatFileSize(selectedFile.size) }}</span>
                    </div>
                </div>
                <div class="upload-progress" v-else>
                    <a-progress
                        :percent="aiStore.uploadProgress"
                        :stroke-color="{
                            '0%': '#1890ff',
                            '100%': '#69c0ff',
                        }"
                        size="small"
                    />
                    <span class="progress-text">正在上传 {{ aiStore.uploadProgress }}%</span>
                </div>
                <div class="file-actions" v-if="!aiStore.isVideoUploading">
                    <button class="upload-btn" @click.stop="uploadFile">
                        <span class="iconfont icon-shangchuan"></span>
                        开始上传
                    </button>
                    <button class="cancel-btn" @click.stop="cancelSelect">
                        取消
                    </button>
                </div>
                <button class="cancel-upload-btn" v-else @click.stop="aiStore.cancelUpload">
                    取消上传
                </button>
            </template>
        </div>

        <div class="link-area" v-else>
            <div class="link-input-wrapper">
                <span class="iconfont icon-lianjie"></span>
                <input
                    type="text"
                    v-model="videoUrl"
                    placeholder="请输入视频链接"
                    class="link-input"
                />
            </div>
            <button class="upload-btn" @click="uploadLink">
                <span class="iconfont icon-shangchuan"></span>
                确认上传
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useAiNoteStore } from '@/stores/aiLearnFriendStore';
import MessageUtil from '@/utils/MessageUtil';

const videoUrl = ref('');
const aiStore = useAiNoteStore();
const selectedFile = ref<File | null>(null);
const mode = ref<'file' | 'link'>('file');

const openFileSelectWindow = () => {
    if (aiStore.isVideoUploading) {
        MessageUtil.info('正在上传视频，请稍后');
        return;
    }
    const input = document.createElement('input') as HTMLInputElement;
    input.type = 'file';
    input.accept = 'video/*';
    input.onchange = (e) => {
        selectedFile.value = (e.target as HTMLInputElement)?.files?.[0] ?? null;
        input.remove();
    };
    input.click();
};

const cancelSelect = () => {
    selectedFile.value = null;
};

const handleDrop = (event: DragEvent) => {
    if (aiStore.isVideoUploading) {
        MessageUtil.info('正在上传视频，请稍后');
        return;
    }
    const fileList = event.dataTransfer?.files;
    if (!fileList || fileList.length == 0) return;
    const file = fileList[0];
    if (!file.type.startsWith('video')) {
        MessageUtil.error('请上传视频文件');
        return;
    }
    selectedFile.value = file;
    uploadFile();
};

const uploadFile = () => {
    if (!selectedFile.value) {
        MessageUtil.error('请先选择文件');
        return;
    }
    aiStore
        .upLoadFile(selectedFile.value!)
        .then((obj) => {
            aiStore.upLoadHistory(
                '视频笔记分析任务' + aiStore.historyList.length + 1,
                obj.url,
                obj.src,
            );
        })
        .catch(() => {
            selectedFile.value = null;
        });
};

const uploadLink = () => {
    if (!videoUrl.value) {
        MessageUtil.error('请输入视频链接');
        return;
    }
    aiStore.upLoadHistory(
        '视频笔记分析任务' + aiStore.historyList.length + 1,
        videoUrl.value,
        videoUrl.value,
    );
};

const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB';
};
</script>

<style scoped lang="scss">
.video-upload {
    width: 400px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 16px;
    overflow: hidden;
    box-shadow: 0 4px 20px rgba(24, 144, 255, 0.1);
    border: 1px solid rgba(24, 144, 255, 0.1);

    .mode-tabs {
        display: flex;
        background: linear-gradient(135deg, rgba(24, 144, 255, 0.06), rgba(105, 192, 255, 0.03));
        border-bottom: 1px solid rgba(24, 144, 255, 0.08);

        .tab {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 8px;
            padding: 14px;
            cursor: pointer;
            transition: all 0.3s;
            font-size: 14px;
            color: #666;

            .iconfont {
                font-size: 16px;
            }

            &:hover {
                background: rgba(24, 144, 255, 0.08);
                color: #1890ff;
            }

            &.active {
                background: linear-gradient(135deg, #1890ff, #69c0ff);
                color: #fff;

                .iconfont {
                    color: #fff;
                }
            }
        }
    }

    .drop-area {
        padding: 32px 24px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.3s;
        min-height: 200px;

        &:hover {
            background: rgba(24, 144, 255, 0.04);

            .upload-icon {
                transform: scale(1.1);
                box-shadow: 0 8px 30px rgba(24, 144, 255, 0.4);
            }
        }

        .upload-icon {
            width: 72px;
            height: 72px;
            border-radius: 50%;
            background: linear-gradient(135deg, #1890ff, #69c0ff);
            display: flex;
            align-items: center;
            justify-content: center;
            margin-bottom: 20px;
            box-shadow: 0 6px 24px rgba(24, 144, 255, 0.35);
            transition: all 0.3s;

            .iconfont {
                font-size: 32px;
                color: #fff;
            }
        }

        .upload-text {
            text-align: center;
            margin-bottom: 12px;

            h4 {
                margin: 0 0 6px 0;
                font-size: 16px;
                font-weight: 600;
                color: #333;
            }

            p {
                margin: 0;
                font-size: 13px;
                color: #999;
            }
        }

        .formats {
            font-size: 11px;
            color: #bbb;
            background: #f5f5f5;
            padding: 4px 12px;
            border-radius: 12px;
        }

        .file-selected {
            display: flex;
            align-items: center;
            gap: 14px;
            margin-bottom: 16px;

            .file-icon {
                width: 48px;
                height: 48px;
                border-radius: 12px;
                background: linear-gradient(135deg, rgba(24, 144, 255, 0.12), rgba(105, 192, 255, 0.08));
                display: flex;
                align-items: center;
                justify-content: center;

                .iconfont {
                    font-size: 24px;
                    color: #1890ff;
                }
            }

            .file-info {
                display: flex;
                flex-direction: column;
                gap: 4px;

                .file-name {
                    font-size: 14px;
                    font-weight: 500;
                    color: #333;
                    max-width: 200px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }

                .file-size {
                    font-size: 12px;
                    color: #999;
                }
            }
        }

        .upload-progress {
            width: 100%;
            max-width: 280px;
            margin-bottom: 12px;

            .progress-text {
                display: block;
                text-align: center;
                font-size: 12px;
                color: #1890ff;
                margin-top: 8px;
            }
        }

        .file-actions, .cancel-upload-btn {
            display: flex;
            gap: 12px;
            margin-top: 8px;
        }

        .upload-btn {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 10px 24px;
            background: linear-gradient(135deg, #1890ff, #69c0ff);
            color: #fff;
            border: none;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s;
            box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);

            .iconfont {
                font-size: 14px;
            }

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
            }
        }

        .cancel-btn, .cancel-upload-btn {
            padding: 10px 20px;
            background: #f5f5f5;
            color: #666;
            border: 1px solid #e8e8e8;
            border-radius: 20px;
            font-size: 13px;
            cursor: pointer;
            transition: all 0.3s;

            &:hover {
                background: #e8e8e8;
            }
        }
    }

    .link-area {
        padding: 24px;
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 16px;

        .link-input-wrapper {
            width: 100%;
            display: flex;
            align-items: center;
            gap: 10px;
            padding: 12px 16px;
            background: #f8fbff;
            border: 1px solid rgba(24, 144, 255, 0.2);
            border-radius: 12px;
            transition: all 0.3s;

            &:focus-within {
                border-color: #1890ff;
                box-shadow: 0 0 0 3px rgba(24, 144, 255, 0.1);
            }

            .iconfont {
                font-size: 18px;
                color: #1890ff;
            }

            .link-input {
                flex: 1;
                border: none;
                background: transparent;
                font-size: 14px;
                color: #333;
                outline: none;

                &::placeholder {
                    color: #bbb;
                }
            }
        }

        .upload-btn {
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 12px 32px;
            background: linear-gradient(135deg, #1890ff, #69c0ff);
            color: #fff;
            border: none;
            border-radius: 24px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s;
            box-shadow: 0 4px 16px rgba(24, 144, 255, 0.3);

            .iconfont {
                font-size: 16px;
            }

            &:hover {
                transform: translateY(-2px);
                box-shadow: 0 6px 20px rgba(24, 144, 255, 0.4);
            }
        }
    }
}
</style>