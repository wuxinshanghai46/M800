<template>
  <el-card class="table-card">
    <!-- ===== 列表视图 ===== -->
    <template v-if="!detail">
      <div class="page-header">
        <div>
          <h2>通道管理</h2>
          <div class="page-desc">通道须绑定供应商与国家，维护连接参数。<span style="color:#9ca3af;">活跃通道 = 开关开启 + 连接正常，系统路由时只选择活跃通道。</span></div>
        </div>
        <div style="display:flex;gap:8px;">
          <el-button @click="openImport"><el-icon><Upload /></el-icon>批量导入</el-button>
          <el-button type="primary" @click="openForm()">＋ 新建通道</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <div class="filter-item">
          <label>通道 ID</label>
          <el-input v-model="query.channelCode" placeholder="如 CH001" clearable style="width:120px;" @keyup.enter="loadData" />
        </div>
        <div class="filter-item">
          <label>通道名称</label>
          <el-input v-model="query.keyword" placeholder="搜索通道名称" clearable style="width:180px;" @keyup.enter="loadData" />
        </div>
        <div class="filter-item">
          <label>所属国家</label>
          <el-select v-model="query.countryCode" placeholder="全部" clearable style="width:150px;">
            <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} ${c.code}`" :value="c.code" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>所属供应商</label>
          <el-select v-model="query.vendorId" placeholder="全部" clearable filterable style="width:160px;">
            <el-option v-for="v in vendorOptions" :key="v.id" :label="v.vendorName" :value="v.id" />
          </el-select>
        </div>
        <div class="filter-item">
          <label>状态</label>
          <el-select v-model="query.filterStatus" placeholder="全部" clearable style="width:100px;">
            <el-option label="启用" :value="true" />
            <el-option label="关闭" :value="false" />
          </el-select>
        </div>
        <div class="filter-actions">
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="query.channelCode='';query.keyword='';query.countryCode='';query.vendorId='';query.filterStatus='';loadData()">重置</el-button>
        </div>
      </div>

      <el-card shadow="never" style="border:1px solid #e5e7eb;">
        <template #header>
          <span style="font-size:14px;font-weight:600;color:#1f2937;">通道列表 <span style="color:#9ca3af;font-weight:400;font-size:12px;">共 {{ total }} 条</span></span>
        </template>
        <el-table :data="filteredList" v-loading="loading" :header-cell-style="headerStyle">
          <el-table-column prop="channelCode" label="通道 ID" width="110">
            <template #default="{row}"><code class="mono-code gray">{{ row.channelCode }}</code></template>
          </el-table-column>
          <el-table-column prop="channelName" label="通道名称" width="150">
            <template #default="{row}"><span style="font-weight:500;">{{ row.channelName }}</span></template>
          </el-table-column>
          <el-table-column prop="countryCode" label="所属国家" width="130" align="center">
            <template #default="{row}">{{ getCountryLabel(row.countryCode) }}</template>
          </el-table-column>
          <el-table-column label="供应商" width="140">
            <template #default="{row}">{{ getVendorName(row.vendorId) }}</template>
          </el-table-column>
          <el-table-column label="协议" width="80" align="center">
            <template #default="{row}"><el-tag :type="row.channelType===1?'':'warning'" size="small" effect="plain">{{ row.channelType===1?'SMPP':'HTTP' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="通道方向" width="100" align="center">
            <template #default="{row}">
              <el-tag size="small" :type="row.smppBindType==='BIND_RX'?'warning':row.smppBindType==='BIND_TX'?'success':'primary'" effect="light">
                {{ row.smppBindType==='BIND_RX'?'上行MO':row.smppBindType==='BIND_TX'?'下行MT':'双向' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="updatedAt" label="更新时间" width="110" align="center">
            <template #default="{row}"><span style="font-size:12px;color:#6b7280;">{{ (row.updatedAt||row.createdAt||'').substring(0,10) }}</span></template>
          </el-table-column>
          <el-table-column prop="priority" label="优先级" width="80" align="center">
            <template #default="{row}"><span style="font-size:12px;">{{ row.priority ?? '-' }}</span></template>
          </el-table-column>
          <el-table-column label="连接状态" width="100" align="center">
            <template #default="{row}">
              <template v-if="connStatusMap[row.id] !== undefined">
                <el-tag v-if="connStatusMap[row.id]==='online'" type="success" size="small" effect="light">● 在线</el-tag>
                <el-tag v-else type="danger" size="small" effect="light">● 失败</el-tag>
              </template>
              <template v-else>
                <el-tag v-if="row.status===1" type="success" size="small" effect="light">● 在线</el-tag>
                <el-tag v-else-if="row.status===0" type="danger" size="small" effect="light">● 离线</el-tag>
                <el-tag v-else-if="row.status===2" type="warning" size="small" effect="light">测试中</el-tag>
                <el-tag v-else type="info" size="small" effect="light">未知</el-tag>
              </template>
            </template>
          </el-table-column>
          <el-table-column label="开关" width="80" align="center">
            <template #default="{row}"><el-tag :type="row.isActive?'success':'info'" size="small" effect="light" round>{{ row.isActive ? '启用' : '关闭' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{row}">
              <el-button link type="primary" size="small" @click="testConn(row)" :loading="testingMap[row.id]">测试连通</el-button>
              <el-button link type="primary" size="small" @click="openDetail(row)">配置</el-button>
              <el-dropdown trigger="click" style="margin-left:4px;" @command="cmd => onCmd(cmd, row)">
                <el-button link size="small" style="color:#374151;">···</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">{{ row.isActive ? '关闭' : '启用' }}</el-dropdown-item>
                    <el-dropdown-item command="delete" divided><span style="color:#dc2626;">删除</span></el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" :page-sizes="[10,20,50]" layout="total, sizes, prev, pager, next" @change="loadData" style="margin-top:12px;" />
      </el-card>
    </template>

    <!-- ===== 通道配置视图 ===== -->
    <template v-else>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
        <div style="display:flex;align-items:center;gap:12px;">
          <el-button @click="detail=null;loadData()" plain>← 返回列表</el-button>
          <span style="font-size:14px;font-weight:600;" >通道配置 — {{ detail.channelName }}</span>
        </div>
        <el-button type="primary" @click="saveConfig" :loading="saving">保存所有配置</el-button>
      </div>

      <el-tabs v-model="detailTab" type="card">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="detail" label-width="110px" label-position="right" style="max-width:700px;padding:16px 0;">
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="通道名称"><el-input v-model="detail.channelName" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="所属供应商">
                <el-select v-model="detail.vendorId" filterable style="width:100%;">
                  <el-option v-for="v in vendorOptions" :key="v.id" :label="`${v.vendorName} (${v.countryCode||''})`" :value="v.id" />
                </el-select>
              </el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="所属国家">
                <el-select v-model="detail.countryCode" filterable style="width:100%;">
                  <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} ${c.code}`" :value="c.code" />
                </el-select>
              </el-form-item></el-col>
              <el-col :span="12"><el-form-item label="路由优先级">
                <el-input-number v-model="detail.priority" :min="1" :max="999" style="width:100%;" />
                <div class="form-hint">数值越小优先级越高，同国家多通道时按此排序</div>
              </el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="人工开关">
                <el-switch v-model="detail.isActive" active-text="启用" inactive-text="关闭" />
              </el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- SMPP 连接配置 -->
        <el-tab-pane label="SMPP 连接配置" name="smpp" v-if="detail.channelType===1">
          <el-alert type="info" :closable="false" style="margin-bottom:20px;">平台作为 SMPP Client 主动连接供应商服务器，以下为连接参数。</el-alert>
          <el-form :model="detail" label-width="160px" style="max-width:750px;">
            <div class="section-title">服务器连接</div>
            <el-form-item label="服务器地址（支持FQDN）"><el-input v-model="detail.smppHost" placeholder="域名或IP" /><div class="form-hint">支持域名自动解析</div></el-form-item>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="端口"><el-input-number v-model="detail.smppPort" :min="1" :max="65535" style="width:100%;" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="System ID（账号）"><el-input v-model="detail.smppSystemId" /></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="Password"><el-input v-model="detail.smppPassword" type="password" show-password :maxlength="8" /><div class="form-hint" :style="detail.smppPassword && detail.smppPassword.length > 8 ? 'color:#f56c6c' : ''">SMPP 协议限制最多 8 个字符（{{ detail.smppPassword ? detail.smppPassword.length : 0 }}/8）</div></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="System Type"><el-input v-model="detail.smppSystemType" placeholder="可选" /></el-form-item></el-col>
            </el-row>
            <div class="section-title">协议参数</div>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="绑定类型">
                  <el-select v-model="detail.smppBindType" style="width:100%;">
                    <el-option label="BIND_TRX（收发）" value="BIND_TRX" />
                    <el-option label="BIND_TX（仅发送）" value="BIND_TX" />
                    <el-option label="BIND_RX（仅接收）" value="BIND_RX" />
                  </el-select>
                  <div class="form-hint">通常使用 BIND_TRX，部分供应商要求指定类型</div>
                </el-form-item>
              </el-col>
              <el-col :span="12"><el-form-item label="心跳间隔（秒）"><el-input-number v-model="detail.smppEnquireLinkInterval" :min="5" style="width:100%;" /><div class="form-hint">enquire_link 发送间隔</div></el-form-item></el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="目标地址 TON">
                  <el-select v-model="detail.smppDestTon" style="width:100%;">
                    <el-option label="INTERNATIONAL（国际格式）" value="INTERNATIONAL" />
                    <el-option label="NATIONAL（本地格式）" value="NATIONAL" />
                    <el-option label="UNKNOWN" value="UNKNOWN" />
                    <el-option label="ALPHANUMERIC" value="ALPHANUMERIC" />
                  </el-select>
                  <div class="form-hint">目标号码地址类型，默认 INTERNATIONAL</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="目标地址 NPI">
                  <el-select v-model="detail.smppDestNpi" style="width:100%;">
                    <el-option label="ISDN（E.164）" value="ISDN" />
                    <el-option label="UNKNOWN" value="UNKNOWN" />
                    <el-option label="NATIONAL" value="NATIONAL" />
                    <el-option label="PRIVATE" value="PRIVATE" />
                  </el-select>
                  <div class="form-hint">号码计划，默认 ISDN</div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="窗口大小"><el-input-number v-model="detail.smppWindowSize" :min="1" style="width:100%;" /></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="TPS 上限"><el-input-number v-model="detail.tps" :min="0" style="width:100%;" /><div class="form-hint">每秒最大发送条数，0=不限</div></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- HTTP接口配置 -->
        <el-tab-pane label="HTTP接口配置" name="http" v-if="detail.channelType===2">
          <el-form :model="detail" label-width="120px" style="max-width:750px;">
            <el-form-item label="请求URL"><el-input v-model="detail.httpUrl" placeholder="https://api.example.com/send" /></el-form-item>
            <el-form-item label="请求方法">
              <el-radio-group v-model="detail.httpMethod"><el-radio value="POST">POST</el-radio><el-radio value="GET">GET</el-radio></el-radio-group>
            </el-form-item>
            <el-form-item label="请求头"><el-input v-model="detail.httpHeaders" type="textarea" :rows="3" placeholder='{"Authorization":"Bearer xxx"}' /></el-form-item>
            <el-form-item label="请求体模板"><el-input v-model="detail.httpBodyTemplate" type="textarea" :rows="5" placeholder='{"to":"${to}","content":"${content}"}' /></el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 短信参数 -->
        <el-tab-pane label="短信参数" name="sms">
          <el-form :model="detail" label-width="140px" style="max-width:700px;">
            <div class="section-title">分段设置</div>
            <el-row :gutter="16">
              <el-col :span="12"><el-form-item label="最大允许段数"><el-input-number v-model="detail.maxSegments" :min="1" :max="20" style="width:100%;" /><div class="form-hint">超出该段数时拒绝发送</div></el-form-item></el-col>
              <el-col :span="12"><el-form-item label="默认消息有效期(秒)"><el-input-number v-model="detail.dlrWaitTimeout" :min="10" style="width:100%;" /><div class="form-hint">86400=24小时</div></el-form-item></el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- 绑定客户 -->
        <el-tab-pane label="绑定客户" name="customers">
          <el-alert type="info" :closable="false" style="margin-bottom:16px;">以下客户将此通道设置为自定义专用通道，仅在"自定义通道调度模式"下生效。</el-alert>
          <el-empty description="暂无绑定客户" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- ===== 新建通道 ===== -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑通道' : '新建通道'" width="620px" destroy-on-close>
      <el-alert v-if="!form.id" type="info" :closable="false" style="margin-bottom:16px;">创建通道后，请在通道配置页填写 SMPP 连接参数和短信参数。</el-alert>
      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
        <el-form-item label="通道名称" prop="channelName">
          <el-input v-model="form.channelName" placeholder="如 TH-DTAC-01" />
        </el-form-item>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="所属供应商" prop="vendorId">
              <el-select v-model="form.vendorId" filterable placeholder="请选择供应商" style="width:100%;" @change="onVendorChange">
                <el-option v-for="v in vendorOptions" :key="v.id" :label="`${v.vendorName} (${v.countryCode||''})`" :value="v.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务国家" prop="countryCode">
              <el-select v-model="form.countryCode" filterable placeholder="选择供应商后自动填充" style="width:100%;">
                <el-option v-for="c in countryOptions" :key="c.code" :label="`${c.name} ${c.code}`" :value="c.code" />
              </el-select>
              <div class="form-hint">选择供应商后自动继承所属国家，也可手动修改</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="协议类型" prop="channelType">
              <el-select v-model="form.channelType" style="width:100%;"><el-option label="SMPP" :value="1" /><el-option label="HTTP" :value="2" /></el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="通道方向">
              <el-select v-model="form.smppBindType" style="width:100%;">
                <el-option label="双向（MT发送 + MO接收）" value="BIND_TRX" />
                <el-option label="下行MT（仅发送）" value="BIND_TX" />
                <el-option label="上行MO（仅接收上行）" value="BIND_RX" />
              </el-select>
              <div class="form-hint">上行MO通道用于接收客户回复消息</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="路由优先级">
              <el-input-number v-model="form.priority" :min="1" :max="999" style="width:100%;" />
              <div class="form-hint">数值越小优先级越高，默认10</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人工开关">
              <el-switch v-model="form.isActive" active-text="启用" inactive-text="关闭" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible=false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ form.id ? '保存' : '创建并配置' }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 -->
    <el-dialog v-model="importVisible" title="批量导入通道" width="860px" destroy-on-close :close-on-click-modal="false">
      <div class="import-steps">
        <div :class="['step-item', importStep>=1?'active':'', importStep>1?'done':'']"><div class="step-circle">1</div><div class="step-label">上传文件</div></div>
        <div :class="['step-line', importStep>1?'done':'']"></div>
        <div :class="['step-item', importStep>=2?'active':'', importStep>2?'done':'']"><div class="step-circle">2</div><div class="step-label">数据预览</div></div>
        <div :class="['step-line', importStep>2?'done':'']"></div>
        <div :class="['step-item', importStep>=3?'active':'']"><div class="step-circle">3</div><div class="step-label">导入结果</div></div>
      </div>

      <!-- Step 1 -->
      <div v-if="importStep===1">
        <el-alert type="info" :closable="false" style="margin-bottom:16px;">导入文件支持<strong>通道配置</strong>及关联的<strong>SID信息</strong>一并导入。请先下载模板，按格式填写后上传。</el-alert>
        <el-row :gutter="12" style="margin-bottom:20px;">
          <el-col :span="12"><div class="sheet-card"><div class="sheet-title">📡 Sheet 1 — 通道配置</div><div class="sheet-body">通道名称 <span class="req">*</span><br>关联供应商名称 <span class="req">*</span><br>服务国家代码 <span class="req">*</span><br>协议类型（SMPP/HTTP）<span class="req">*</span><br>SMPP host / port / system_id / password</div></div></el-col>
          <el-col :span="12"><div class="sheet-card"><div class="sheet-title">🔖 Sheet 2 — SID 信息（可选）</div><div class="sheet-body">SID 值 <span class="req">*</span><br>关联通道名称 <span class="req">*</span><br>格式类型（ALPHA/NUMERIC/SHORTCODE）<span class="req">*</span><br>短信属性（营销/通知/ALL）<span class="req">*</span></div></div></el-col>
        </el-row>
        <el-upload drag :auto-upload="false" :limit="1" accept=".xlsx,.xls,.csv" :on-change="f=>importFile=f?.raw" :on-remove="()=>importFile=null" style="margin-bottom:16px;">
          <el-icon style="font-size:36px;color:#c0c4cc;"><Upload /></el-icon>
          <div style="margin-top:8px;">点击上传或拖拽文件至此处</div>
          <template #tip><div style="color:#909399;font-size:12px;">支持 .xlsx / .xls / .csv 格式，单次最大 5MB</div></template>
        </el-upload>
        <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:#6b7280;">
          冲突处理策略：
          <el-radio-group v-model="importConflict"><el-radio value="skip">跳过重复（通道名称相同视为重复）</el-radio><el-radio value="overwrite">覆盖更新</el-radio></el-radio-group>
        </div>
      </div>

      <!-- Step 2 -->
      <div v-if="importStep===2">
        <el-row :gutter="12" style="margin-bottom:16px;">
          <el-col :span="6"><div class="pv-stat green"><div class="pv-num">{{ pvSummary.ok }}</div><div class="pv-label">可导入</div></div></el-col>
          <el-col :span="6"><div class="pv-stat orange"><div class="pv-num">{{ pvSummary.warn }}</div><div class="pv-label">警告（可导入）</div></div></el-col>
          <el-col :span="6"><div class="pv-stat red"><div class="pv-num">{{ pvSummary.err }}</div><div class="pv-label">错误（将跳过）</div></div></el-col>
          <el-col :span="6"><div class="pv-stat gray"><div class="pv-num" style="font-size:16px;">{{ pvData.channel.length }} / {{ pvData.sid.length }}</div><div class="pv-label">通道 / SID</div></div></el-col>
        </el-row>
        <el-radio-group v-model="pvSheet" size="small" style="margin-bottom:12px;">
          <el-radio-button value="channel">通道（{{ pvData.channel.length }}条）</el-radio-button>
          <el-radio-button value="sid">SID（{{ pvData.sid.length }}条）</el-radio-button>
        </el-radio-group>
        <el-table :data="pvData[pvSheet]" size="small" :header-cell-style="headerStyle" max-height="260" stripe>
          <el-table-column type="index" label="#" width="50" />
          <template v-if="pvSheet==='channel'">
            <el-table-column prop="channelName" label="通道名称" />
            <el-table-column prop="vendorName" label="供应商" width="120" />
            <el-table-column prop="countryCode" label="国家" width="70" />
            <el-table-column prop="channelType" label="协议" width="80" />
          </template>
          <template v-if="pvSheet==='sid'">
            <el-table-column prop="sid" label="SID值" />
            <el-table-column prop="channelName" label="通道" width="120" />
            <el-table-column prop="sidType" label="格式" width="100" />
          </template>
          <el-table-column prop="_msg" label="状态" width="180">
            <template #default="{row}"><el-tag :type="row._st==='ok'?'success':row._st==='warn'?'warning':'danger'" size="small" effect="light">{{ row._msg }}</el-tag></template>
          </el-table-column>
        </el-table>
        <el-alert type="warning" :closable="false" style="margin-top:16px;"><strong>导入规则：</strong>错误行将自动跳过；关联的供应商不存在时，对应行也将跳过；导入后可在详情页进一步编辑 SMPP 密码等敏感参数。</el-alert>
      </div>

      <!-- Step 3 -->
      <div v-if="importStep===3" style="text-align:center;padding:20px 0;">
        <div style="font-size:56px;margin-bottom:12px;">✅</div>
        <div style="font-size:18px;font-weight:700;color:#111827;margin-bottom:8px;">导入完成</div>
        <div style="font-size:13px;color:#6b7280;margin-bottom:24px;">以下数据已成功写入系统</div>
        <el-row :gutter="16" style="max-width:360px;margin:0 auto 24px;">
          <el-col :span="12"><div class="rc blue"><div class="rc-n">{{ importResult.channels }}</div><div class="rc-l">通道导入</div></div></el-col>
          <el-col :span="12"><div class="rc purple"><div class="rc-n">{{ importResult.sids }}</div><div class="rc-l">SID导入</div></div></el-col>
        </el-row>
        <div v-if="importResult.skipped" style="font-size:13px;color:#d97706;background:#fffbeb;padding:10px 16px;border-radius:6px;display:inline-block;">
          ⚠ {{ importResult.skipped }} 条数据因重复或错误已跳过
        </div>
      </div>

      <template #footer>
        <div style="display:flex;justify-content:space-between;width:100%;">
          <div><el-button v-if="importStep===1" size="small" @click="downloadTemplate"><el-icon><Download /></el-icon>下载模板</el-button></div>
          <div style="display:flex;gap:8px;">
            <el-button @click="importVisible=false">{{ importStep===3?'关闭':'取消' }}</el-button>
            <el-button v-if="importStep===1" type="primary" :disabled="!importFile" @click="parseFile">下一步：预览数据</el-button>
            <el-button v-if="importStep===2" @click="importStep=1">上一步</el-button>
            <el-button v-if="importStep===2" type="primary" @click="doImport" :loading="importing">确认导入</el-button>
            <el-button v-if="importStep===3" type="primary" @click="importVisible=false;loadData()">完成</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Upload, Download } from '@element-plus/icons-vue'
import { channelList, channelSave, channelUpdate, channelDel, channelTestSmpp, channelTestHttp } from '../api'
import { useRefData } from '../stores/refData'
import isoCountries from '../data/countries'
import { getCountryLabel } from '../utils/country'

const refData = useRefData()

const headerStyle = { background: '#f0f2f5', color: '#374151', fontWeight: 600, fontSize: '12px' }
const loading = ref(false); const saving = ref(false)
const list = ref([]); const total = ref(0)
const query = reactive({ page: 1, size: 10, channelCode: '', keyword: '', countryCode: '', vendorId: '', filterStatus: '' })
const dialogVisible = ref(false); const form = ref({}); const formRef = ref(null)
const detail = ref(null); const detailTab = ref('basic')
const vendorOptions = ref([]); const countryOptions = ref([])

const formRules = {
  channelName: [{ required: true, message: '通道名称不能为空', trigger: 'blur' }],
  vendorId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  channelType: [{ required: true, message: '请选择协议类型', trigger: 'change' }],
  countryCode: [{ required: true, message: '请选择或确认所属国家', trigger: 'change' }],
}

const loadOptions = async () => {
  const [v, c] = await Promise.all([refData.loadVendors(), refData.loadCountries()])
  vendorOptions.value = v; countryOptions.value = c
}
const getVendorName = (id) => { const v = vendorOptions.value.find(x => x.id === id); return v ? v.vendorName : id || '-' }

const filteredList = computed(() => {
  let d = list.value
  if (query.channelCode) d = d.filter(c => c.channelCode && c.channelCode.toLowerCase().includes(query.channelCode.toLowerCase()))
  if (query.countryCode) d = d.filter(c => c.countryCode === query.countryCode)
  if (query.vendorId) d = d.filter(c => c.vendorId === query.vendorId)
  if (query.filterStatus !== '' && query.filterStatus !== null) d = d.filter(c => c.isActive === query.filterStatus)
  return d
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.vendorId) params.vendorId = query.vendorId
    if (query.countryCode) params.countryCode = query.countryCode
    const r = await channelList(params)
    list.value = r.data?.list || []; total.value = r.data?.total || 0
  } finally { loading.value = false }
}

const onVendorChange = (vid) => {
  const v = vendorOptions.value.find(x => x.id === vid)
  if (v) { form.value.countryCode = v.countryCode || '' }
}

const openForm = (row, preset) => {
  if (row) { form.value = { ...row } }
  else { form.value = { channelType: 1, isActive: true, status: 1, priority: 10, ...(preset || {}) } }
  loadOptions(); dialogVisible.value = true
}

const handleSave = async () => {
  if (formRef.value) { const v = await formRef.value.validate().catch(() => false); if (!v) return }
  // 强校验：必须有 countryCode
  if (!form.value.countryCode) { ElMessage.warning('请选择服务国家'); return }
  saving.value = true
  try {
    if (!form.value.id && !form.value.channelCode) form.value.channelCode = 'CH' + Date.now().toString(36).toUpperCase()
    const res = form.value.id ? await channelUpdate(form.value.id, form.value) : await channelSave(form.value)
    refData.refresh('channels')
    ElMessage.success('保存成功'); dialogVisible.value = false
    if (!form.value.id && res.data) { openDetail(res.data) } else { loadData() }
  } finally { saving.value = false }
}

const onCmd = (cmd, row) => {
  if (cmd === 'toggle') toggleStatus(row)
  else if (cmd === 'delete') handleDel(row.id)
}
const handleDel = async id => {
  try {
    await ElMessageBox.confirm('确认删除该通道？如有通道组引用将无法删除。', '删除确认', { type: 'warning' })
    await channelDel(id); ElMessage.success('删除成功'); loadData()
  } catch (e) { if (e !== 'cancel') throw e }
}
const toggleStatus = async row => {
  const newActive = !row.isActive
  try { await channelUpdate(row.id, { ...row, isActive: newActive }); ElMessage.success(newActive ? '已启用' : '已关闭'); loadData() } catch(e) {/* */}
}

const testingMap = reactive({})
const connStatusMap = reactive({})
const connTimeMap = reactive({})

const testConn = async (row) => {
  testingMap[row.id] = true
  try {
    const res = row.channelType === 1
      ? await channelTestSmpp(row.id)
      : await channelTestHttp(row.id)
    const d = res.data || {}
    const ok = d.success === true
    connStatusMap[row.id] = ok ? 'online' : 'offline'
    connTimeMap[row.id] = '最近检测 刚刚'
    if (ok) {
      ElMessage.success(`${row.channelName} — 连接成功`)
    } else {
      const hint = d.hint ? `\n提示：${d.hint}` : ''
      ElNotification({
        title: `${row.channelName} — 连接失败`,
        message: (d.message || '连接失败，请检查配置') + hint,
        type: 'error',
        duration: 8000
      })
    }
  } catch (e) {
    connStatusMap[row.id] = 'offline'
    ElMessage.error(`${row.channelName} — 测试失败: ${e.message || '网络错误'}`)
  } finally {
    testingMap[row.id] = false
  }
}

const openDetail = row => { detail.value = { ...row }; detailTab.value = 'basic'; loadOptions() }
const saveConfig = async () => {
  saving.value = true
  try { await channelUpdate(detail.value.id, detail.value); ElMessage.success('配置已保存') } finally { saving.value = false }
}

// ===== Import =====
const importVisible = ref(false); const importStep = ref(1); const importFile = ref(null)
const importConflict = ref('skip'); const importing = ref(false)
const pvSheet = ref('channel')
const pvSummary = ref({ ok: 0, warn: 0, err: 0 })
const pvData = ref({ channel: [], sid: [] })
const importResult = ref({ channels: 0, sids: 0, skipped: 0 })

const openImport = () => { importStep.value = 1; importFile.value = null; importVisible.value = true }
const downloadTemplate = () => {
  const csv = '通道名称,供应商名称,国家代码,协议类型,SMPP Host,SMPP Port,System ID,Password\n'
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'channel_import_template.csv'; a.click()
  ElMessage.success('模板已下载')
}
const parseFile = async () => {
  if (!importFile.value) return
  const text = await importFile.value.text()
  const lines = text.split('\n').map(l => l.trim()).filter(l => l && !l.startsWith('通道名称'))
  const channels = []; let ok = 0, warn = 0, err = 0
  const existingNames = list.value.map(c => c.channelName)
  const vendorNames = vendorOptions.value.map(v => v.vendorName)
  for (const line of lines) {
    const c = line.split(',').map(s => s.trim())
    const row = { channelName: c[0]||'', vendorName: c[1]||'', countryCode: c[2]||'', channelType: c[3]||'SMPP', smppHost: c[4]||'', smppPort: c[5]||'', systemId: c[6]||'', _st: 'ok', _msg: '新增' }
    if (!c[0] || !c[1] || !c[2]) { row._st = 'err'; row._msg = '错误：必填字段为空'; err++ }
    else if (!vendorNames.includes(c[1])) { row._st = 'err'; row._msg = '错误：供应商不存在'; err++ }
    else if (existingNames.includes(c[0])) { row._st = 'warn'; row._msg = '已存在，将跳过'; warn++ }
    else { ok++ }
    channels.push(row)
  }
  pvData.value = { channel: channels, sid: [] }; pvSummary.value = { ok, warn, err }; pvSheet.value = 'channel'; importStep.value = 2
}
const doImport = async () => {
  importing.value = true; let cc = 0, sk = 0
  for (const ch of pvData.value.channel) {
    if (ch._st === 'err' || (ch._st === 'warn' && importConflict.value === 'skip')) { sk++; continue }
    const vendor = vendorOptions.value.find(v => v.vendorName === ch.vendorName)
    if (!vendor) { sk++; continue }
    try {
      await channelSave({
        channelName: ch.channelName, channelCode: 'CH' + Date.now().toString(36).toUpperCase() + cc,
        vendorId: vendor.id, countryCode: ch.countryCode,
        channelType: ch.channelType === 'HTTP' ? 2 : 1,
        smppHost: ch.smppHost, smppPort: ch.smppPort ? Number(ch.smppPort) : null,
        smppSystemId: ch.systemId, status: 1, isActive: true
      }); cc++
    } catch(e) { sk++ }
  }
  importResult.value = { channels: cc, sids: 0, skipped: sk }; importing.value = false; importStep.value = 3
}

const route = useRoute()
onMounted(() => {
  if (route.query.vendorId) query.vendorId = Number(route.query.vendorId)
  if (route.query.countryCode) query.countryCode = route.query.countryCode
  loadOptions(); loadData()
})
</script>

<style scoped>
.mono-code { font-family: 'Courier New', monospace; font-size: 13px; }
.mono-code.gray { color: #6b7280; }
.page-desc { font-size: 13px; color: #6b7280; margin-top: 2px; }
.filter-bar { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px 20px; margin-bottom: 16px; display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-item label { font-size: 12px; color: #6b7280; font-weight: 500; }
.filter-actions { display: flex; gap: 8px; margin-left: auto; padding-top: 20px; }
.section-title { font-size: 13px; font-weight: 700; color: #4b5563; padding-bottom: 10px; border-bottom: 1px solid #f3f4f6; margin: 20px 0 16px; display: flex; align-items: center; gap: 8px; }
.section-title::before { content: ''; display: inline-block; width: 3px; height: 14px; background: #2563eb; border-radius: 2px; }
.form-hint { font-size: 11px; color: #9ca3af; margin-top: 2px; }

/* Import */
.import-steps { display: flex; align-items: center; margin-bottom: 28px; padding: 0 40px; }
.step-item { display: flex; flex-direction: column; align-items: center; gap: 6px; }
.step-circle { width: 28px; height: 28px; border-radius: 50%; background: #e5e7eb; color: #6b7280; font-size: 13px; font-weight: 700; display: flex; align-items: center; justify-content: center; }
.step-label { font-size: 11px; color: #9ca3af; white-space: nowrap; }
.step-item.active .step-circle { background: #2563eb; color: #fff; }
.step-item.active .step-label { color: #2563eb; font-weight: 500; }
.step-item.done .step-circle { background: #16a34a; color: #fff; }
.step-item.done .step-label { color: #16a34a; }
.step-line { flex: 1; height: 2px; background: #e5e7eb; margin: 0 8px; margin-bottom: 18px; min-width: 60px; }
.step-line.done { background: #16a34a; }
.sheet-card { border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px; }
.sheet-title { font-size: 12px; font-weight: 700; color: #374151; margin-bottom: 6px; }
.sheet-body { font-size: 12px; color: #6b7280; line-height: 1.8; }
.req { color: #dc2626; }
.pv-stat { padding: 12px; border-radius: 8px; text-align: center; }
.pv-stat.green { background: #f0fdf4; border: 1px solid #bbf7d0; }
.pv-stat.orange { background: #fffbeb; border: 1px solid #fcd34d; }
.pv-stat.red { background: #fef2f2; border: 1px solid #fca5a5; }
.pv-stat.gray { background: #f3f4f6; border: 1px solid #e5e7eb; }
.pv-num { font-size: 22px; font-weight: 700; }
.pv-stat.green .pv-num { color: #16a34a; } .pv-stat.orange .pv-num { color: #d97706; } .pv-stat.red .pv-num { color: #dc2626; } .pv-stat.gray .pv-num { color: #374151; }
.pv-label { font-size: 12px; color: #6b7280; margin-top: 4px; }
.rc { padding: 16px; border-radius: 8px; text-align: center; }
.rc.blue { background: #eff6ff; } .rc.purple { background: #f5f3ff; }
.rc-n { font-size: 28px; font-weight: 700; }
.rc.blue .rc-n { color: #2563eb; } .rc.purple .rc-n { color: #7c3aed; }
.rc-l { font-size: 12px; color: #6b7280; margin-top: 4px; }
</style>
