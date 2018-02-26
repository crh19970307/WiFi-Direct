//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// This code is licensed under the MIT License (MIT).
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;
using Windows.Devices.Enumeration;
using System.ComponentModel;
using Windows.Devices.WiFiDirect;
using System.Threading.Tasks;
using Windows.UI.Popups;
using Windows.UI.Core;
using Windows.Foundation;
using Windows.Storage;
using Windows.Storage.Pickers;
using Windows.Storage.Provider;
using Windows.ApplicationModel.Core;

namespace SDKTemplate
{
    public class SocketReaderWriter : IDisposable
    {
        DataReader _dataReader;
        DataWriter _dataWriter;
        StreamSocket _streamSocket;
        private MainPage _rootPage;

        public SocketReaderWriter(StreamSocket socket, MainPage mainPage)
        {
            _dataReader = new DataReader(socket.InputStream);
            _dataReader.UnicodeEncoding = UnicodeEncoding.Utf8;
            _dataReader.ByteOrder = ByteOrder.LittleEndian;

            _dataWriter = new DataWriter(socket.OutputStream);
            _dataWriter.UnicodeEncoding = UnicodeEncoding.Utf8;
            _dataWriter.ByteOrder = ByteOrder.LittleEndian;

            _streamSocket = socket;
            _rootPage = mainPage;
        }

        public void Dispose()
        {
            _dataReader.Dispose();
            _dataWriter.Dispose();
            _streamSocket.Dispose();
        }

        public async Task WriteMessageAsync(string message)
        {
            try
            {
                _dataWriter.WriteUInt32(_dataWriter.MeasureString(message));
                _dataWriter.WriteString(message);
                await _dataWriter.StoreAsync();
                _rootPage.NotifyUserFromBackground("Sent message: " + message, NotifyType.StatusMessage);
            }
            catch (Exception ex)
            {
                _rootPage.NotifyUserFromBackground("WriteMessage threw exception: " + ex.Message, NotifyType.StatusMessage);
            }
        }

        public async Task<string> ReadMessageAsync()
        {
            try
            {
                UInt32 bytesRead = await _dataReader.LoadAsync(sizeof(UInt32));
                if (bytesRead > 0)
                {
                    // Determine how long the string is.
                    UInt32 messageLength = _dataReader.ReadUInt32();
                    bytesRead = await _dataReader.LoadAsync(messageLength);
                    if (bytesRead > 0)
                    {
                        // Decode the string.
                        string message = _dataReader.ReadString(messageLength);
                        _rootPage.NotifyUserFromBackground("Got message: " + message, NotifyType.StatusMessage);
                        return message;
                    }
                }
            }
            catch (Exception)
            {
                _rootPage.NotifyUserFromBackground("Socket was closed!", NotifyType.StatusMessage);
            }
            return null;
        }
        public async Task SendFileAsync(StorageFile file)
        {
            _rootPage.NotifyUser("testing", NotifyType.ErrorMessage);
            
            
            try
            {
                _rootPage.NotifyUser("testing", NotifyType.ErrorMessage);
                byte[] message = null;
                uint len=0;
                using (IRandomAccessStreamWithContentType stream = await file.OpenReadAsync())
                {
                    len =( uint)(stream.Size);
                    message = new byte[stream.Size];
                    using (DataReader reader = new DataReader(stream))
                    {
                        await reader.LoadAsync((uint)stream.Size);
                        reader.ReadBytes(message);
                    }
                }
                //Message is the byte array

                _dataWriter.WriteUInt32(len);
                _dataWriter.WriteBytes(message);
                await _dataWriter.StoreAsync();
                _rootPage.NotifyUserFromBackground("Sent file: " + file.Name, NotifyType.StatusMessage);
            }
            catch (Exception ex)
            {
                _rootPage.NotifyUserFromBackground("Send File threw exception: " + ex.Message, NotifyType.StatusMessage);
            }
            
        }
        public async Task<string> ReadFileAsync()
        {
            try
            {
                UInt32 bytesRead = await _dataReader.LoadAsync(sizeof(UInt32));
                if (bytesRead > 0)
                {
                    // Determine how long the string is.
                    
                    UInt32 messageLength = _dataReader.ReadUInt32();
                    bytesRead = await _dataReader.LoadAsync(messageLength);
                    if (bytesRead > 0)
                    {
                        // Decode the string.
                        byte[] value;
                        value = new byte[messageLength];
                        _dataReader.ReadBytes(value);
                        //_dataReader.ReadBytes(messageLength);


                        //Windows.Storage.StorageFolder storageFolder = Windows.Storage.ApplicationData.Current.LocalFolder;
                        FolderPicker openFile = new FolderPicker();
                        openFile.SuggestedStartLocation = PickerLocationId.DocumentsLibrary;
                        openFile.ViewMode = PickerViewMode.List;
                        openFile.FileTypeFilter.Add(".txt");
                        openFile.FileTypeFilter.Add(".docx");
                        openFile.FileTypeFilter.Add(".pptx");

                        // 选取单个文件

                        StorageFolder storageFolder = await openFile.PickSingleFolderAsync();

                        Windows.Storage.StorageFile sampleFile = await storageFolder.CreateFileAsync("2333", CreationCollisionOption.ReplaceExisting);
                        await Windows.Storage.FileIO.WriteBytesAsync(sampleFile, value);
                        
                        

                        
                        _rootPage.NotifyUserFromBackground("Got file: " , NotifyType.StatusMessage);
                        return "2333";
                    }
                }
            }
            catch (Exception)
            {
                _rootPage.NotifyUserFromBackground("Socket was closed!", NotifyType.StatusMessage);
            }
            return null;
        }
        
    }
    
    public class DiscoveredDevice : INotifyPropertyChanged
    {
        public DeviceInformation DeviceInfo { get; private set; }

        public DiscoveredDevice(DeviceInformation deviceInfo)
        {
            DeviceInfo = deviceInfo;
        }

        public string DisplayName => DeviceInfo.Name + " - " + (DeviceInfo.Pairing.IsPaired ? "Paired" : "Unpaired");
        public override string ToString() => DisplayName;

        public void UpdateDeviceInfo(DeviceInformationUpdate update)
        {
            DeviceInfo.Update(update);
            PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("DisplayName"));
        }

        public event PropertyChangedEventHandler PropertyChanged;
    }

    public class ConnectedDevice : IDisposable
    {
        public SocketReaderWriter SocketRW { get; }
        public WiFiDirectDevice WfdDevice { get; }
        public string DisplayName { get; }

        public ConnectedDevice(string displayName, WiFiDirectDevice wfdDevice, SocketReaderWriter socketRW)
        {
            DisplayName = displayName;
            WfdDevice = wfdDevice;
            SocketRW = socketRW;
        }

        public override string ToString() => DisplayName;

        public void Dispose()
        {
            // Close socket
            SocketRW.Dispose();

            // Close WiFiDirectDevice object
            WfdDevice.Dispose();
        }
    }

}
